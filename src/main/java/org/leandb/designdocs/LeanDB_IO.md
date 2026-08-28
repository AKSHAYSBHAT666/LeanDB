# LeanDB Storage Architecture

## Overview

LeanDB currently has a small disk-storage layer built around three core
concepts:

-   **Block** --- identifies *where* data lives in a table file.
-   **Page** --- holds *what* data is being stored.
-   **DBStorageEngine** --- performs the physical file read/write
    operations.

The architecture separates the **logical identity of a storage
location** from the **bytes stored there** and from the **I/O mechanism
used to persist those bytes**.

``` text
Application
    │
    ▼
StorageClient
    │
    ▼
DBStorageEngine
    │
    ├── Page ──► byte[] data
    │     │
    │     └── Block ──► table file + block number
    │
    ▼
Table File (.tbl)
    │
    ├── Block 0
    ├── Block 1
    ├── Block 2
    └── ...
```

> **Core idea:** `Block → WHERE`, `Page → WHAT`.

------------------------------------------------------------------------

## 1. Block

`Block` represents the **physical identity/location** of a storage block
inside a table file.

It contains:

``` text
Block
├── blk_num     → block number
└── tbl_fl_nm   → table file name/path
```

A block therefore answers:

> **Where should I read or write?**

The block number is used to calculate the byte offset inside the table
file.

The current implementation stores the block number as an `int` and the
table file name as a `String`.

------------------------------------------------------------------------

## 2. Page

`Page` represents the **data associated with a block**.

It contains:

``` text
Page
├── data[]      → bytes to be stored
└── blk         → Block describing the physical location
```

A newly created `Page` allocates a byte array whose size is
`Properties.BLOCK_SIZE`.

Conceptually:

``` text
Page
 ├───────────────┐
 │ byte[] data   │
 │               │
 │ BLOCK_SIZE    │
 └───────────────┘
        │
        ▼
     Block
   ┌───────────┐
   │ file      │
   │ blk_num   │
   └───────────┘
```

The `Page` therefore answers:

> **What data should be stored at this location?**

The intended access pattern is:

``` text
1. Obtain a Block
2. Use the Block to determine the file and block number
3. Calculate the physical offset
4. Read/write the Page data
```

------------------------------------------------------------------------

## 3. DBStorageEngine

`DBStorageEngine` is the layer responsible for **physical disk I/O**.

It currently exposes two operations:

``` java
int fl_write(Page pg)
byte[] fl_read(Block blk)
```

### Write path

`fl_write(Page pg)` receives a `Page`.

It first obtains the associated `Block`:

``` text
Page
 │
 └── Block
      ├── table file
      └── block number
```

The physical file offset is calculated as:

``` text
offset = block_number × BLOCK_SIZE
```

For example, conceptually:

``` text
BLOCK_SIZE = 400

Block 0 → offset 0
Block 1 → offset 400
Block 2 → offset 800
Block 3 → offset 1200
```

The engine then opens the table file using Java's `FileChannel`, creates
a `ByteBuffer` around the page's byte array, and writes it at the
calculated offset.

``` text
Page.data
   │
   ▼
ByteBuffer
   │
   ▼
FileChannel.write(..., offset)
   │
   ▼
table_file.tbl
```

The file is opened with `CREATE` and `WRITE`, so the table file can be
created if it does not already exist.

------------------------------------------------------------------------

## 4. Read path

`fl_read(Block blk)` receives a `Block` rather than a `Page`.

The engine uses the block to determine:

1.  Which table file to open
2.  Which block number to read
3.  Which physical byte offset to use

The offset is again:

``` text
offset = block_number × BLOCK_SIZE
```

A byte buffer of `Properties.BLOCK_SIZE` is allocated and populated
using `FileChannel.read(...)`.

``` text
table_file.tbl
     │
     │ offset
     ▼
┌───────────────┐
│ block bytes   │
└───────────────┘
     │
     ▼
ByteBuffer
     │
     ▼
byte[]
```

The returned value is the raw byte array representing the block's
contents.

------------------------------------------------------------------------

## 5. Physical Storage Model

The current design treats a table file as a sequence of fixed-size
blocks:

``` text
users.tbl

┌──────────────┬──────────────┬──────────────┬──────────────┐
│   Block 0    │   Block 1    │   Block 2    │   Block 3    │
│  BLOCK_SIZE  │  BLOCK_SIZE  │  BLOCK_SIZE  │  BLOCK_SIZE  │
└──────────────┴──────────────┴──────────────┴──────────────┘
      0            1              2              3
   blk_num      blk_num        blk_num        blk_num
```

The block number is not itself a byte offset. It is converted into an
offset by multiplying it by the configured block size.

``` text
Block number
     │
     ▼
block_number × BLOCK_SIZE
     │
     ▼
file byte offset
```

This provides direct positional access to a block without having to scan
the file from the beginning.

------------------------------------------------------------------------

## 6. Separation of Responsibilities

The current architecture deliberately gives each class a different
responsibility.

  Component           Responsibility
  ------------------- ----------------------------------------------
  `Block`             Physical identity/location
  `Page`              In-memory block-sized data
  `DBStorageEngine`   Physical file I/O
  `Properties`        Storage configuration such as `BLOCK_SIZE`
  `StorageClient`     Entry point for exercising the storage layer
  `Main`              Application entry point

The supplied `Main` creates a `StorageClient` and calls `run()`:

``` text
Main
 │
 ▼
StorageClient
 │
 ▼
storage operations
 │
 ▼
DBStorageEngine
 │
 ▼
.tbl files
```

`StorageClient` and `Properties` are referenced by the supplied source
but are not included in the current set of files, so their internal
architecture is not documented here.

------------------------------------------------------------------------

## 7. Concurrency

Both physical I/O methods in `DBStorageEngine` are currently declared:

``` java
synchronized
```

Therefore, access through these methods is serialized per
`DBStorageEngine` instance.

Conceptually:

``` text
Thread A ──► fl_write()
                  │
                  ▼
             DBStorageEngine
                  │
Thread B ──► fl_read()   waits
```

This provides a basic synchronization mechanism around the current file
read/write operations.

The supplied code does not establish a larger transaction, locking,
buffer-pool, or recovery architecture.

------------------------------------------------------------------------

## 8. Current Layering

At its current stage, LeanDB can be viewed as having the following
storage hierarchy:

``` text
┌───────────────────────────────┐
│          Main                 │
│     Application Entry         │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│       StorageClient           │
│     Storage-layer caller      │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│      DBStorageEngine          │
│       Physical I/O            │
│                               │
│   fl_write(Page)              │
│   fl_read(Block)              │
└───────────────┬───────────────┘
                │
        ┌───────┴────────┐
        ▼                ▼
      Page             Block
   "WHAT"             "WHERE"
        │                │
        │                └──────► table file + block number
        │
        └───────────────────────► byte[]
                │
                ▼
          .tbl table file
```

------------------------------------------------------------------------

## 9. Design Philosophy

The important abstraction in the current implementation is the
distinction between **location** and **contents**.

### Block = location

``` text
Block
 ├── table file
 └── block number
```

It identifies a physical storage position.

### Page = contents

``` text
Page
 ├── byte[]
 └── Block
```

It packages the data that belongs at that physical position.

### DBStorageEngine = persistence

``` text
DBStorageEngine
 ├── calculate offset
 ├── open file
 ├── read bytes
 └── write bytes
```

This means higher layers do not need to perform `FileChannel` operations
directly.

------------------------------------------------------------------------

## 10. Current Read/Write API

The current low-level API is intentionally small:

``` java
// Write a page to its associated block
int fl_write(Page pg);

// Read a block from disk
byte[] fl_read(Block blk);
```

The asymmetry is intentional in the current design:

``` text
WRITE
Page
 │
 └── contains Block + data
       │
       ▼
     disk

READ
Block
 │
 ▼
disk
 │
 ▼
byte[]
```

A read can therefore begin with only a `Block`, while a write needs both
the destination location and the data, which are packaged together
inside a `Page`.

------------------------------------------------------------------------

## 11. Future Expansion

The supplied code establishes the physical storage foundation, but
higher-level database functionality is not yet represented in these
files.

Potential future layers could build above this foundation:

``` text
SQL / Query Layer
       │
       ▼
Record / Row Layer
       │
       ▼
File / Table Manager
       │
       ▼
Buffer Manager
       │
       ▼
DBStorageEngine
       │
       ▼
.tbl files
```

Those layers are **not currently implemented in the supplied files**;
this diagram represents a possible architectural direction rather than
existing functionality.

------------------------------------------------------------------------

## Summary

The current LeanDB storage architecture is centered around three
abstractions:

``` text
                 ┌─────────────┐
                 │    Page     │
                 │    WHAT     │
                 │             │
                 │   byte[]    │
                 └──────┬──────┘
                        │
                        │ contains
                        ▼
                 ┌─────────────┐
                 │    Block    │
                 │    WHERE    │
                 │             │
                 │ file + num  │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │ DBStorage   │
                 │   Engine    │
                 │             │
                 │ read/write  │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │  .tbl file  │
                 └─────────────┘
```

The central rule is:

> **Block identifies where data lives; Page represents what is stored
> there; DBStorageEngine handles the physical persistence.**

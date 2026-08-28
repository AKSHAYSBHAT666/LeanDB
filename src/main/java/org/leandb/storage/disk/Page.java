package org.leandb.storage.disk;

import org.leandb.commons.Properties;

/*
Page to hold the byte[] data to be stored
inside the file.

Block → WHERE
Page  → WHAT

the access pattern is to first obtain the block
then calculate the offset to read/write
 */
public class Page
{
    byte[] data;
    Block blk;

    public Page(Block blk)
    {
        this.blk = blk;
        this.data = new byte[Properties.BLOCK_SIZE];
    }

    public byte[] get_data()
    {
        return this.data;
    }

    public Block get_blk()
    {
        return this.blk;
    }

    public void set_data(byte[] data)
    {
        this.data = data;
    }

    public void set_blk(Block blk)
    {
        this.blk = blk;
    }
}


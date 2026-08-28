package org.leandb.storage;

import org.leandb.storage.disk.Block;
import org.leandb.storage.disk.DBStorageEngine;
import org.leandb.storage.disk.Page;

import java.nio.charset.StandardCharsets;

public class StorageClient
{
    public void run()
    {
        DBStorageEngine storageEngine = new DBStorageEngine();

        // 1. Identify where we want to store the page
        Block block = new Block(0, "users.tbl");

        // 2. Create a page belonging to that block
        Page page = new Page(block);

        // 3. Put some data into the page
        byte[] message = "Hello LeanDB".getBytes(StandardCharsets.UTF_8);

        System.arraycopy(
                message,
                0,
                page.get_data(),
                0,
                message.length
        );

        // 4. Write page to disk
        int bytesWritten = storageEngine.fl_write(page);

        System.out.println("Bytes written: " + bytesWritten);

        // 5. Read the block back
        byte[] data = storageEngine.fl_read(block);

        // 6. Convert the beginning of the page back to String
        String result = new String(
                data,
                0,
                message.length,
                StandardCharsets.UTF_8
        );

        System.out.println("Data read: " + result);
    }
}

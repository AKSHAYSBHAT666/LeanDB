package org.leandb.storage;

import org.leandb.commons.storage.Properties;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class DBStorageEngine
{
    public int fl_write(Block blk, Page pg)
    {
        long offset = (long) blk.get_blk_num() * Properties.BLOCK_SIZE;
        Path tbl_fl_nm = Paths.get(blk.get_tbl_fl_nm());

        // 1. Write data at a specific offset
        try (FileChannel fileChannel = FileChannel.open(tbl_fl_nm,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE))
        {
            // Overloaded write method accepts target file position directly
            ByteBuffer w_buff = ByteBuffer.wrap(pg.get_data());
            return fileChannel.write(w_buff, offset);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public byte[] fl_read(Block blk)
    {
        long offset = (long) blk.get_blk_num() * Properties.BLOCK_SIZE;
        Path tbl_fl_nm = Paths.get(blk.get_tbl_fl_nm());

        // 2. Read data from a specific offset
        try (FileChannel fileChannel = FileChannel.open(tbl_fl_nm, StandardOpenOption.READ)) {
            ByteBuffer r_buff = ByteBuffer.allocate(Properties.BLOCK_SIZE);

            // Overloaded read method accepts source file position directly
            fileChannel.read(r_buff, offset);
            return r_buff.array();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

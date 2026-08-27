package org.leandb.storage;

/*
Block class is physical identity/location
inside the file.

Block → WHERE
Page  → WHAT
 */
public class Block
{
    private int blk_num;
    private String tbl_fl_nm;

    Block(int blk_num, String tbl_fl_nm)
    {
        this.blk_num = blk_num;
        this.tbl_fl_nm = tbl_fl_nm;
    }

    public int get_blk_num()
    {
        return this.blk_num;
    }

    public String get_tbl_fl_nm()
    {
        return this.tbl_fl_nm;
    }

    public void set_blk_num(int blk_num)
    {
        this.blk_num = blk_num;
    }

    public void set_tbl_fl_nm(String tbl_fl_nm)
    {
        this.tbl_fl_nm = tbl_fl_nm;
    }
}

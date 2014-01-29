package floobits.common.protocol;

import floobits.common.Buf;

import java.io.Serializable;

public class FlooPatch extends Base {
    public String name = "patch";
    public Integer id;
    public Integer user_id;
    public String md5_after;
    public String md5_before;
    public String patch;

    // Deprecated
    public String path;
    public String username;


    public FlooPatch(){}

    public FlooPatch (String patch, String md5_before, Buf buf) {
        this.path = buf.path;
        this.md5_before = md5_before;
        this.md5_after = buf.md5;
        this.id = buf.id;
        this.patch = patch;
    }
}

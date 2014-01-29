package floobits.common.protocol.send;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class GetBufResponse implements Serializable {
    public String name;
    public Integer id;
    public String path;
    public String buf;
    public String encoding;
    public String md5;
}

package floobits.common.protocol.send;

import floobits.common.protocol.RiBuf;
import floobits.common.protocol.User;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kans on 1/28/14.
 */
public class RoomInfoResponse implements Serializable {
    public String[] anon_perms;
    public Integer max_size;
    public String name;
    public String owner;
    public String[] perms;
    public String room_name;
    public Boolean secret;
    public HashMap<Integer, User> users;
    public HashMap<Integer, RiBuf> bufs;

}

package floobits.common.protocol.send;

import floobits.common.protocol.Base;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.receive.RoomInfoBuf;

import java.util.HashMap;

public class RoomInfoResponse extends Base {
    public String[] anon_perms;
    public Integer max_size;
    public String name;
    public String owner;
    public String[] perms;
    public String room_name;
    public Boolean secret;
    public HashMap<Integer, FlooUser> users;
    public HashMap<Integer, RoomInfoBuf> bufs;

}

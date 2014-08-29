package floobits.common.protocol.json.send;

import floobits.common.protocol.Base;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.json.receive.RoomInfoBuf;

import java.util.HashMap;

public class RoomInfoResponse implements Base {
    public String[] anon_perms;
    public Integer max_size;
    public String owner;
    public String[] perms;
    public String room_name;
    public Boolean secret;
    public HashMap<Integer, FlooUser> users;
    public HashMap<Integer, RoomInfoBuf> bufs;
    public String user_id;

}

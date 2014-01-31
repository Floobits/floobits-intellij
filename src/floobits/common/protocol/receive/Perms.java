package floobits.common.protocol.receive;

import floobits.common.Buf;
import floobits.common.protocol.Base;

import java.util.ArrayList;

public class Perms implements Base {
    public String name = "highlight";
    public String user_id;
    public String[] perms;
    public String action;
}
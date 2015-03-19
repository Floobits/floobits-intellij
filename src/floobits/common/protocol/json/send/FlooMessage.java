package floobits.common.protocol.json.send;

import floobits.common.Utils;
import floobits.common.protocol.Base;

public class FlooMessage implements Base {
    String name = "msg";
    public int req_id = Utils.getRequestId();
    String data;

    public FlooMessage(String chatContents) {
       data = chatContents;
    }
}

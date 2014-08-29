package floobits.common.protocol.json.send;

import floobits.common.protocol.Base;

public class FlooMessage implements Base {
    String name = "msg";
    String data;

    public FlooMessage(String chatContents) {
       data = chatContents;
    }
}

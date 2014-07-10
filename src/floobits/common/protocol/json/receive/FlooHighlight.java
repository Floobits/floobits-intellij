package floobits.common.protocol.json.receive;

import floobits.common.protocol.buf.Buf;
import floobits.common.protocol.Base;

import java.util.ArrayList;

public class FlooHighlight implements Base {
    public String name = "highlight";
    public Integer id;
    public Boolean ping = false;
    public Boolean summon = false;
    public Boolean following = false;
    public ArrayList<ArrayList<Integer>> ranges;
    public Integer user_id;

    public FlooHighlight (Buf buf, ArrayList<ArrayList<Integer>> ranges, Boolean summon, Boolean following) {
        this.following = following;
        this.id = buf.id;
        if (summon != null) {
            this.summon = summon;
            this.ping = summon;
        }
        this.ranges = ranges;
    }
}

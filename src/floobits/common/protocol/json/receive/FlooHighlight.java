package floobits.common.protocol.json.receive;

import floobits.common.Utils;
import floobits.common.protocol.Base;
import floobits.common.protocol.buf.Buf;

import java.util.ArrayList;

public class FlooHighlight implements Base {
    public String name = "highlight";
    public int req_id = Utils.getRequestId();
    public Integer id;
    public Boolean ping = false;
    public Boolean summon = false;
    public Boolean following = false;
    public ArrayList<ArrayList<Integer>> ranges;
    public Integer user_id;
    public ArrayList<String> to;

    public FlooHighlight (Buf buf, ArrayList<ArrayList<Integer>> ranges, Boolean summon, Boolean following) {
        this.following = following;
        this.id = buf.id;
        if (summon != null) {
            this.summon = summon;
            this.ping = summon;
        }
        this.ranges = ranges;
    }

    public FlooHighlight (Buf buf, ArrayList<ArrayList<Integer>> ranges, Boolean summon, Boolean following, ArrayList<String> to) {
        this.following = following;
        this.id = buf.id;
        if (summon != null) {
            this.summon = summon;
            this.ping = summon;
        }
        this.ranges = ranges;
        this.to = to;
    }

    public FlooHighlight (Integer id, ArrayList<ArrayList<Integer>> ranges, Boolean summon, Boolean following, Integer userId) {
        this.following = following;
        this.id = id;
        this.summon = summon;
        this.ping = summon;
        this.ranges = ranges;
        this.user_id = userId;
    }
}

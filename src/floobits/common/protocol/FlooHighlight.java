package floobits.common.protocol;

import floobits.common.Buf;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by kans on 1/28/14.
 */
public class FlooHighlight implements Serializable {
    public String name = "highlight";
    public Integer id;
    public Boolean ping = false;
    public Boolean summon = false;
    public ArrayList<ArrayList<Integer>> ranges;
    public Integer user_id;

    public FlooHighlight(){}

    public FlooHighlight (Buf buf, ArrayList<ArrayList<Integer>> ranges, Boolean summon) {
        this.id = buf.id;
        if (summon != null) {
            this.summon = summon;
            this.ping = summon;
        }
        this.ranges = ranges;
    }
}

package floobits.common.protocol.receive;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.common.protocol.Base;

import java.util.HashMap;
import java.util.Map;

public class RoomInfoTree implements Base {
    public HashMap<String, Integer> bufs;
    public HashMap<String, RoomInfoTree> folders;
    public RoomInfoTree(JsonObject obj) {
        this.bufs = new HashMap<String, Integer>();
        this.folders = new HashMap<String, RoomInfoTree>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                this.bufs.put(key, Integer.parseInt(value.getAsString()));
            } else {
                this.folders.put(key, new RoomInfoTree(value.getAsJsonObject()));
            }
        }
    }
}

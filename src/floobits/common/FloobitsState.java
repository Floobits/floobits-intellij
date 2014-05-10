package floobits.common;

import com.google.gson.JsonObject;
import floobits.FlooContext;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.send.RoomInfoResponse;
import floobits.utilities.Flog;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by kans on 5/7/14.
 */
public class FloobitsState {
    public JsonObject lastHighlight;
    public Boolean stalking = false;
    public HashSet<String> perms = new HashSet<String>();
    private Map<Integer, FlooUser> users = new HashMap<Integer, FlooUser>();
    HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    final HashMap<String, Integer> paths_to_ids = new HashMap<String, Integer>();
    private int connectionId;

    public boolean readOnly = false;
    public String username = "";
    protected HashSet<Integer> requests = new HashSet<Integer>();
    private FlooContext context;
    public FlooUrl url;

    public FloobitsState(FlooContext context, FlooUrl flooUrl) {
        this.context = context;
        url = flooUrl;
    }

    public boolean can(String perm) {
        if (!context.isJoined())
            return false;

        if (!perms.contains(perm)) {
            Flog.info("we can't do that because perms");
            return false;
        }
        return true;
    }

    public void handleRoomInfo(RoomInfoResponse ri) {
        users = ri.users;
        context.chatManager.setUsers(users);
        perms = new HashSet<String>(Arrays.asList(ri.perms));
        if (!can("patch")) {
            readOnly = true;
            context.statusMessage("You don't have permission to edit files in this workspace.  All documents will be set to read-only.", false);
        }
        connectionId = Integer.parseInt(ri.user_id);
        Flog.info("Got roominfo with userId %d", connectionId);

    }
    public void set_buf_path(Buf buf, String newPath) {
        paths_to_ids.remove(buf.path);
        buf.path = newPath;
        paths_to_ids.put(buf.path, buf.id);
    }

    public @Nullable
    Buf get_buf_by_path(String absPath) {
        String relPath = context.toProjectRelPath(absPath);
        if (relPath == null) {
            return null;
        }
        Integer id = paths_to_ids.get(FilenameUtils.separatorsToUnix(relPath));
        if (id == null) {
            return null;
        }
        return bufs.get(id);
    }
    public String getUsername(int userId) {
        FlooUser user = users.get(userId);
        if (user == null) {
            return "";
        }
        return user.username;
    }

    /**
     * Get a user by their connection id (userId).
     * @param userId
     * @return null or the FlooUser object for the connection id.
     */
    public FlooUser getUser(int userId) {
        return users.get(userId);
    }

    public void addUser(FlooUser flooser) {
        users.put(flooser.user_id, flooser);
        context.statusMessage(String.format("%s joined the workspace on %s (%s).", flooser.username, flooser.platform, flooser.client), false);
        context.chatManager.setUsers(users);
    }

    public void removeUser(int userId) {
        FlooUser u = users.get(userId);
        users.remove(userId);
        context.statusMessage(String.format("%s left the workspace.", u.username), false);
        context.chatManager.setUsers(this.users);
    }
    public int getMyConnectionId() {
        return connectionId;
    }

    public void changePermsForUser(int userId, String[] permissions) {
        FlooUser user = getUser(userId);
        if (user == null) {
            return;
        }
        List<String> givenPerms = java.util.Arrays.asList(permissions);
        Set<String> translatedPermsSet = new HashSet<String>();
        HashMap<String, String[]> permTypes = new HashMap<String, String[]>();
        permTypes.put("edit_room", new String[]{
                "patch", "get_buf", "set_buf", "create_buf", "delete_buf", "rename_buf", "set_temp_data", "delete_temp_data",
                "highlight", "msg", "datamsg", "create_term", "term_stdin", "delete_term", "update_term", "term_stdout", "saved"
        });
        permTypes.put("view_room", new String[]{"get_buf", "ping", "pong"});
        permTypes.put("request_perms", new String[]{"get_buf", "request_perms"});
        permTypes.put("admin_room", new String[]{"kick", "pull_repo", "perms"});
        for (Map.Entry<String, String[]> entry : permTypes.entrySet()) {
            if (givenPerms.contains(entry.getKey())) {
                for (String perm : entry.getValue()) {
                    translatedPermsSet.add(perm);
                }
            }
        }
        user.perms = translatedPermsSet.toArray(new String[translatedPermsSet.size()]);
    }

    public void shutdown() {
        bufs = null;
    }
}

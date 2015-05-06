package floobits.common;

import com.google.gson.JsonObject;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.buf.Buf;
import floobits.common.protocol.json.receive.FlooHighlight;
import floobits.common.protocol.json.send.RoomInfoResponse;
import floobits.utilities.Flog;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ScheduledFuture;


public class FloobitsState {
    public FlooHighlight lastHighlight;
    public HashMap<String, FlooHighlight> lastUserHighlights = new HashMap<String, FlooHighlight>();
    private Boolean following = false;
    private ScheduledFuture pausedFollowing;
    public HashSet<String> perms = new HashSet<String>();
    public Map<Integer, FlooUser> users = new HashMap<Integer, FlooUser>();
    HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    final HashMap<String, Integer> pathsToIds = new HashMap<String, Integer>();
    private int connectionId;

    public boolean readOnly = false;
    public String username = "";
    private IContext context;
    public FlooUrl url;
    public List<String> followedUsers = new ArrayList<String>();

    public FloobitsState(IContext context, FlooUrl flooUrl) {
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
        Flog.info("Got roominfo with userId %d", connectionId);
        users = ri.users;
        perms = new HashSet<String>(Arrays.asList(ri.perms));
        if (!can("patch")) {
            readOnly = true;
            context.statusMessage("You don't have permission to edit files in this workspace.  All documents will be set to read-only.");
        }
        connectionId = Integer.parseInt(ri.user_id);
        for (FlooUser user : ri.users.values()) {
            context.addUser(user);
        }

    }
    public void setBufPath(Buf buf, String newPath) {
        pathsToIds.remove(buf.path);
        buf.path = FilenameUtils.separatorsToUnix(newPath);
        pathsToIds.put(buf.path, buf.id);
    }

    public @Nullable
    Buf getBufByPath(String absPath) {
        String relPath = context.toProjectRelPath(absPath);
        if (relPath == null) {
            return null;
        }
        Integer id = pathsToIds.get(FilenameUtils.separatorsToUnix(relPath));
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

    public String getGravatar(int userId) {
        FlooUser user = users.get(userId);
        if (user == null) {
            return "";
        }
        return user.gravatar;
    }

    /**
     * Get a user by their connection id (userId).
     * @param userId
     * @return null or the FlooUser object for the connection id.
     */
    public FlooUser getUser(int userId) {
        return users.get(userId);
    }

    public void addUser(FlooUser user) {
        users.put(user.user_id, user);
        context.addUser(user);
    }

    public void removeUser(int userId) {
        FlooUser u = users.get(userId);
        if (u == null) {
            return;
        }
        context.removeUser(u);
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
                Collections.addAll(translatedPermsSet, entry.getValue());
            }
        }
        user.perms = translatedPermsSet.toArray(new String[translatedPermsSet.size()]);
    }

    public void shutdown() {
        bufs = null;
    }

    public Boolean getFollowing() {
        return following;
    }

    public void setFollowing(Boolean following) {
        this.pauseFollowing(false);
        this.following = following;
        if (!following) {
            this.followedUsers.clear();
            if (context.getFlooHandler() == null) {
                return;
            }
            context.updateFollowing();
        }
    }

    public Boolean getPausedFollowing() {
        return pausedFollowing != null;
    }

    public void pauseFollowing(Boolean pauseFollowing) {
        /*
        Possible states:
            Not following, not paused.
            Following, not paused.
            Following, paused.

        Impossible state:
            Not following, paused. Paused means following, just not active. Anytime we set follow, we must cancel any pause.
         */
        if (this.pausedFollowing != null) {
            following = true;
            this.pausedFollowing.cancel(true);
        }
        this.pausedFollowing = null;
        if (pauseFollowing) {
            if (!following) {
                return;
            }
            following = false;
            this.pausedFollowing = context.setTimeout(2000, new Runnable() {
                @Override
                public void run() {
                    pauseFollowing(false);
                }
            });
        }
    }

    public void setFollowedUsers(List<String> followedUsers) {
        this.followedUsers = followedUsers;
        setFollowing(followedUsers.size() > 0);
        context.updateFollowing();
    }

    public int numBufs() {
        return pathsToIds.size();
    }
}

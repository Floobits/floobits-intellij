package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import floobits.FlooContext;
import floobits.common.handlers.BaseHandler;
import floobits.common.handlers.FlooHandler;
import floobits.utilities.Flog;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class API {
    public static class CrashDump implements Serializable {
        public String owner;
        public String workspace;
        public String dir;
        public String subject;
        public String username;
        public String useragent;
        public HashMap<String, String> message = new HashMap<String, String>();

        public CrashDump(Throwable e, String owner, String workspace, String dir, String username) {
            this.owner = owner;
            this.workspace = workspace;
            this.dir = dir;
            this.username = username;
            message.put("sendingAt", String.format("%s", new Date().getTime()));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            message.put("stack", sw.toString());
            message.put("description", e.getMessage());
            setContextInfo("%s died%s!");
        }

        public CrashDump(String description, String username) {
            this.username = username;
            message.put("sendingAt", String.format("%s", new Date().getTime()));
            message.put("description", description);
            setContextInfo("%s submitted an issues%s!");
        }

        protected void setContextInfo(String subjectText) {
            ApplicationInfo instance = ApplicationInfo.getInstance();
            IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("com.floobits.unique.plugin.id"));
            String version = plugin != null ? plugin.getVersion() : "???";
            useragent = String.format("%s-%s-%s %s (%s-%s)", instance.getVersionName(), instance.getMajorVersion(), instance.getMinorVersion(), version, System.getProperty("os.name"), System.getProperty("os.version"));
            subject = String.format(subjectText, instance.getVersionName(), username != null ? " for " + username : "");
        }
    }


    public static boolean createWorkspace(String host, String owner, String workspace, FlooContext context, boolean notPublic) {
        PostMethod method;

        method = new PostMethod("/api/workspace");
        Gson gson = new Gson();
        String json = gson.toJson(new HTTPWorkspaceRequest(owner, workspace, notPublic));
        try {
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            apiRequest(method, context, host);
        } catch (IOException e) {
            context.errorMessage(String.format("Could not create workspace %s/%s: %s", owner, workspace, e.toString()));
            return false;
        }

        int code = method.getStatusCode();
        switch (code) {
            case 400:
                // Todo: pick a new name or something
                context.errorMessage("Invalid workspace name (a-zA-Z0-9).");
                return false;
            case 402:
                String details;
                try {
                    String res = method.getResponseBodyAsString();
                    JsonObject obj = (JsonObject)new JsonParser().parse(res);
                    details = obj.get("detail").getAsString();
                } catch (IOException e) {
                    Flog.warn(e);
                    return false;
                }
                context.errorMessage(details);
                return false;
            case 409:
                context.statusMessage("The workspace already exists so I am joining it.");
            case 201:
                context.statusMessage("Workspace created.");
                return true;
            case 401:
                Flog.log("Auth failed");
                context.errorMessage("There is an invalid username or secret in your ~/.floorc and you were not able to authenticate.");
                return context.openFile(new File(Settings.floorcJsonPath));
            default:
                try {
                    Flog.warn(String.format("Unknown error creating workspace:\n%s", method.getResponseBodyAsString()));
                } catch (IOException e) {
                    Flog.warn(e);
                }
                return false;
        }
    }
    public static boolean updateWorkspace(final FlooUrl f, HTTPWorkspaceRequest workspaceRequest, FlooContext context) {

        PutMethod method = new PutMethod(String.format("/api/workspace/%s/%s", f.owner, f.workspace));
        Gson gson = new Gson();
        String json = gson.toJson(workspaceRequest);
        try {
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            apiRequest(method, context, f.host);
        } catch (IOException e) {
            context.errorMessage(String.format("Could not create workspace: %s", e.toString()));
            return false;
        }
        String responseBodyAsString;
        try {
            responseBodyAsString = method.getResponseBodyAsString();
        } catch (IOException e) {
            Flog.warn(e);
            return false;
        }
        Flog.log(responseBodyAsString);
        return method.getStatusCode() < 300;
    }

    public static HTTPWorkspaceRequest getWorkspace(final FlooUrl f, FlooContext context) {

        HttpMethod method;
        try {
            method = getWorkspaceMethod(f, context);
        } catch (IOException e) {
            return null;
        }
        String responseBodyAsString;
        try {
            responseBodyAsString = method.getResponseBodyAsString();
        } catch (IOException e) {
            return null;
        }
        // Redirects aren't followed, so die here
        if (method.getStatusCode() >= 300) {
            PersistentJson.removeWorkspace(f);
            return null;
        }
        return new Gson().fromJson(responseBodyAsString, (Type) HTTPWorkspaceRequest.class);
    }

    public static boolean workspaceExists(final FlooUrl f, FlooContext context) {
        if (f == null) {
            return false;
        }
        HttpMethod method;
        try {
            method = getWorkspaceMethod(f, context);
        } catch (Throwable e) {
            Flog.warn(e);
            return false;
        }

        if (method.getStatusCode() >= 300){
            PersistentJson.removeWorkspace(f);
            return false;
        }
        return true;
    }

    static private HttpMethod getWorkspaceMethod(FlooUrl f, FlooContext context) throws IOException {
        return apiRequest(new GetMethod(String.format("/api/workspace/%s/%s", f.owner, f.workspace)), context, f.host);
    }

    static public HttpMethod apiRequest(HttpMethod method, FlooContext context, String host) throws IOException, IllegalArgumentException {
        Flog.info("Sending an API request");
        final HttpClient client = new HttpClient();
        // NOTE: we cant tell java to follow redirects because they can fail.
        HttpConnectionManager connectionManager = client.getHttpConnectionManager();
        HttpConnectionParams connectionParams = connectionManager.getParams();
        connectionParams.setParameter("http.protocol.handle-redirects", true);
        connectionParams.setSoTimeout(5000);
        connectionParams.setConnectionTimeout(3000);
        connectionParams.setIntParameter(HttpMethodParams.BUFFER_WARN_TRIGGER_LIMIT, 1024 * 1024);
        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Throwable e) {
            Flog.warn(e);
        }
        HashMap<String, String> auth = floorcJson != null ? floorcJson.auth.get(host) : null;
        String username = null, secret = null;
        if (auth != null) {
            username = auth.get("username");
            secret = auth.get("secret");
        }
        if (username == null) {
            username = "";
        }
        if (secret == null) {
            secret = "";
        }
        HttpClientParams params = client.getParams();
        params.setAuthenticationPreemptive(true);
        client.setParams(params);
        Credentials credentials = new UsernamePasswordCredentials(username, secret);
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getHostConfiguration().setHost(host, 443, new Protocol("https", new SSLProtocolSocketFactory() {
            @Override
            public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
                return Utils.createSSLContext().getSocketFactory().createSocket(socket, s, i, b);
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException {
                return Utils.createSSLContext().getSocketFactory().createSocket(s, i, inetAddress, i2);
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i2, HttpConnectionParams httpConnectionParams) throws IOException, ConnectTimeoutException {
                return Utils.createSSLContext().getSocketFactory().createSocket(s, i, inetAddress, i2);
            }

            @Override
            public Socket createSocket(String s, int i) throws IOException {
                return Utils.createSSLContext().getSocketFactory().createSocket(s, i);
            }
        }, 443));

        client.executeMethod(method);
        return method;
    }

    static public List<String> getOrgsCanAdmin(String host, FlooContext context) {
        final GetMethod method = new GetMethod("/api/orgs/can/admin");
        List<String> orgs = new ArrayList<String>();

        try {
            apiRequest(method, context, host);
        } catch (Throwable e) {
            Flog.warn(e);
            return orgs;
        }

        if (method.getStatusCode() >= 400) {
            return orgs;
        }

        try {
            String response = method.getResponseBodyAsString();
            JsonArray orgsJson = new JsonParser().parse(response).getAsJsonArray();
    
            for (int i = 0; i < orgsJson.size(); i++) {
                JsonObject org = orgsJson.get(i).getAsJsonObject();
                String orgName = org.get("name").getAsString();
                orgs.add(orgName);
            }
        } catch (Throwable e) {
            Flog.warn(e);
            context.errorMessage("Error getting Floobits organizations. Try again later or please contact support.");
        }

        return orgs;
    }
    static public void uploadCrash(BaseHandler baseHandler, final FlooContext context, Throwable throwable) {
        try {
            Flog.warn("Uploading crash report: %s", throwable);
            final PostMethod method;
            String owner = "";
            String workspace = "";
            String colabDir = "";
            String username = "";

            if (baseHandler != null) {
                owner = baseHandler.getUrl().owner;
                workspace = baseHandler.getUrl().workspace;
                colabDir = context != null ? context.colabDir : "???";
                username = baseHandler instanceof FlooHandler ? ((FlooHandler)baseHandler).state.username : "???";
            }
            method = new PostMethod("/api/log");
            Gson gson = new Gson();
            CrashDump crashDump = new CrashDump(throwable, owner, workspace, colabDir, username);
            String json = gson.toJson(crashDump);
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        apiRequest(method, context, Constants.floobitsDomain);
                    } catch (Throwable e) {
                        Utils.errorMessage(String.format("Couldn't send crash report %s", e), context != null ? context.project : null);
                    }
                }
            }, "Floobits Crash Reporter").run();
      } catch (Throwable e) {
            try {
                Utils.errorMessage(String.format("Couldn't send crash report %s", e), context != null ? context.project : null);
            } catch (Throwable ignored) {}
        }
    }
    static public void uploadCrash(FlooContext context, Throwable throwable) {
        uploadCrash(context.handler, context, throwable);
    }
    static public void sendUserIssue(String description, String username) {
        final PostMethod method;
        method = new PostMethod("/api/log");
        Gson gson = new Gson();
        CrashDump crashDump = new CrashDump(String.format("User submitted an issue: %s", description), username);
        String json = gson.toJson(crashDump);
        try {
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Flog.warn("Couldn't send a user issue.");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    apiRequest(method, null, Constants.floobitsDomain);
                } catch (Throwable e) {
                    Utils.errorMessage(String.format("Couldn't send crash report %s", e), null);
                }
            }
        }, "Floobits User Issue Submitter").run();
    }
}

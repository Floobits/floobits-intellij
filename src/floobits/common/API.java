package floobits.common;

import com.google.gson.*;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.ProcessCanceledException;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.BaseHandler;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.common.protocol.json.send.CodeReviewRequest;
import floobits.impl.ApplicationImpl;
import floobits.utilities.Flog;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class API {
    public static  int maxErrorReports = 3;
    private static int numSent = 0;
    private static String UA = String.format("Floo.%s %s-%s.%s (%s)", Constants.version, ApplicationImpl.getClientName(),
            ApplicationInfo.getInstance().getMajorVersion(), ApplicationInfo.getInstance().getMinorVersion(), System.getProperty("os.name"));

    public static boolean createWorkspace(String host, String owner, String workspace, IContext context, boolean notPublic) {
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
                    Flog.error(e);
                    return false;
                }
                context.errorMessage(String.format("%s Get more %s.", details,
                        Utils.getLinkHTML(String.format("https://%s/%s/settings#billing", host, owner), "here")));
                return false;
            case 409:
                context.statusMessage("The workspace already exists so I am joining it.");
                return true;
            case 201:
                context.statusMessage("Workspace created.");
                return true;
            case 401:
                Flog.log("Auth failed");
                context.errorMessage("There is an invalid username or secret in your ~/.floorc and you were not able to authenticate.");
                return context.iFactory.openFile(new File(Settings.floorcJsonPath));
            default:
                String errorMessage = "Unknown error.";
                try {
                    errorMessage = method.getResponseBodyAsString();
                } catch (IOException e) {
                    Flog.error(e);
                }
                context.errorMessage(String.format("Error creating workspace: %s", errorMessage));
                Flog.error(String.format("Unknown error creating workspace:\n%s", errorMessage));
                return false;
        }
    }
    public static String requestReview(final FlooUrl f, String description, IContext context) {

        PostMethod method = new PostMethod(String.format("/api/workspace/%s/%s/review", f.owner, f.workspace));
        Gson gson = new Gson();
        CodeReviewRequest request = new CodeReviewRequest(description);
        String json = gson.toJson(request);

        try {
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            apiRequest(method, context, f.host);
        } catch (IOException e) {
            context.errorMessage(String.format("Could not request code review: %s", e.toString()));
            return e.toString();
        }
        String responseBodyAsString;
        JsonElement message;
        try {
            responseBodyAsString = method.getResponseBodyAsString();
            JsonObject jsonObject = new Gson().fromJson(responseBodyAsString, JsonObject.class);
            message = jsonObject.get("message");
        } catch (Throwable e) {
            Flog.error(e);
            return e.toString();
        }
        return message.getAsString();
    }

    public static boolean updateWorkspace(final FlooUrl f, HTTPWorkspaceRequest workspaceRequest, IContext context) {

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
            Flog.error(e);
            return false;
        }
        Flog.log(responseBodyAsString);
        return method.getStatusCode() < 300;
    }

    public static HTTPWorkspaceRequest getWorkspace(final FlooUrl f, IContext context) {

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

    public static FlooUserDetail getUserDetail(IContext context, FloobitsState state) {

        HttpMethod method;

        String defaultHhost = Utils.getDefaultHost();
        try {
            method = apiRequest(new GetMethod(String.format("/api/user/%s", state.username)), context, defaultHhost);
        } catch (IOException e) {
            return null;
        }
        String responseBodyAsString;
        try {
            responseBodyAsString = method.getResponseBodyAsString();
        } catch (IOException e) {
            return null;
        }
        if (method.getStatusCode() >= 300) {
            return null;
        }
        return new Gson().fromJson(responseBodyAsString, (Type) FlooUserDetail.class);
    }

    public static boolean workspaceExists(final FlooUrl f, IContext context) {
        if (f == null) {
            return false;
        }
        HttpMethod method;
        try {
            method = getWorkspaceMethod(f, context);
        } catch (Throwable e) {
            Flog.error(e);
            return false;
        }
        // TODO: this could be an auth error or 500 or 503. Return more useful error message in that case
        if (method.getStatusCode() >= 300) {
            PersistentJson.removeWorkspace(f);
            return false;
        }
        return true;
    }

    static private HttpMethod getWorkspaceMethod(FlooUrl f, IContext context) throws IOException {
        return apiRequest(new GetMethod(String.format("/api/workspace/%s/%s", f.owner, f.workspace)), context, f.host);
    }

    static public HttpMethod apiRequest(HttpMethod method, IContext context, String host) throws IOException, IllegalArgumentException {
        Flog.info("Sending an API request");
        final HttpClient client = new HttpClient();

        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Throwable e) {
            Flog.error(e);
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

        params.setParameter("http.protocol.handle-redirects", true);
        params.setParameter("http.useragent", UA);
        params.setConnectionManagerTimeout(10000);
        params.setIntParameter(HttpMethodParams.BUFFER_WARN_TRIGGER_LIMIT, 1024 * 1024);
        client.setParams(params);

        Credentials credentials = new UsernamePasswordCredentials(username, secret);
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getHostConfiguration().setHost(host, 443, new Protocol("https", new SSLProtocolSocketFactory() {
            @Override
            public Socket createSocket(Socket sock, String s, int i, boolean b) throws IOException {
                return Utils.createSSLContext().getSocketFactory().createSocket(sock, s, i, b);
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException {
                Socket socket = Utils.createSSLContext().getSocketFactory().createSocket();
                socket.bind(new InetSocketAddress(inetAddress, i2));
                socket.connect(new InetSocketAddress(s, i), 10000);
                return socket;
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i2, HttpConnectionParams httpConnectionParams) throws IOException {
                Socket socket = Utils.createSSLContext().getSocketFactory().createSocket();
                socket.bind(new InetSocketAddress(inetAddress, i2));
                socket.connect(new InetSocketAddress(s, i), 10000);
                return socket;
            }

            @Override
            public Socket createSocket(String s, int i) throws IOException {
                return Utils.createSSLContext().getSocketFactory().createSocket(s, i);
            }
        }, 443));

        client.executeMethod(method);
        return method;
    }

    static public List<String> getOrgsCanAdmin(String host, IContext context) {
        final GetMethod method = new GetMethod("/api/orgs/can/admin");
        List<String> orgs = new ArrayList<String>();

        try {
            apiRequest(method, context, host);
        } catch (Throwable e) {
            Flog.error(e);
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
            Flog.error(e);
            context.errorMessage("Error getting Floobits organizations. Try again later or please contact support.");
        }

        return orgs;
    }
    static public void uploadCrash(BaseHandler baseHandler, final IContext context, Throwable throwable) {
        numSent++;
        if (numSent >= maxErrorReports) {
            Flog.warn(String.format("Already sent %s errors this session. Not sending any more.", numSent));
            if (throwable != null) Flog.error(throwable);
            return;
        }
        if (throwable instanceof ProcessCanceledException) {
            Flog.warn("Process canceled.");
            return;
        }
        if (throwable instanceof java.lang.OutOfMemoryError) {
            Flog.warn("Out of memory.");
            return;
        }
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
                username = baseHandler instanceof FlooHandler ? ((FlooHandler) baseHandler).state.username : "???";
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
                        if (context != null) {
                            context.errorMessage(String.format("Couldn't send crash report %s", e));
                        }

                    }
                }
            }, "Floobits Crash Reporter").run();
        } catch (Throwable e) {
            if (context == null) {
                Flog.warn(String.format("Couldn't send crash report %s", e));
                return;
            }
            try {
                context.errorMessage(String.format("Couldn't send crash report %s", e));
            } catch (Throwable ignored) {}
        }
    }
    static public void uploadCrash(IContext context, Throwable throwable) {
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
                    Flog.errorMessage(String.format("Couldn't send crash report %s", e), null);
                }
            }
        }, "Floobits User Issue Submitter").run();
    }
}

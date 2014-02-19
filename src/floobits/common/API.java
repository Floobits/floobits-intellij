package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.utilities.Flog;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class API {

    public static boolean createWorkspace(String owner, String workspace, FlooContext context, boolean notPublic) {
        PostMethod method;

        method = new PostMethod("/api/workspace/");
        Gson gson = new Gson();
        String json = gson.toJson(new HTTPWorkspaceRequest(owner, workspace, notPublic));
        try {
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            apiRequest(method, context);
        } catch (IOException e) {
            context.error_message(String.format("Could not create workspace: %s", e.toString()));
            return false;
        }

        int code = method.getStatusCode();
        switch (code) {
            case 400:
                // Todo: pick a new name or something
                context.error_message("Invalid workspace name (a-zA-Z0-9).");
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
                context.error_message(details);
                return false;
            case 409:
                context.status_message("The workspace already exists so I am joining it.");
            case 201:
                context.status_message("Workspace created.");
                return true;
            case 401:
                Flog.log("Auth failed");
                context.error_message("There is an invalid username or secret in your ~/.floorc and you were not able to authenticate.");
                VirtualFile floorc = LocalFileSystem.getInstance().findFileByIoFile(new File(Settings.floorcPath));
                if (floorc == null) {
                    return false;
                }
                FileEditorManager.getInstance(context.project).openFile(floorc, true);
                return false;
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

        PutMethod method = new PutMethod(String.format("/api/workspace/%s/%s/", f.owner, f.workspace));
        Gson gson = new Gson();
        String json = gson.toJson(workspaceRequest);
        try {
            method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
            apiRequest(method, context);
        } catch (IOException e) {
            context.error_message(String.format("Could not create workspace: %s", e.toString()));
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
            method = API.getWorkspace(f.owner, f.workspace, context);
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
            method = API.getWorkspace(f.owner, f.workspace, context);
        } catch (IOException e) {
            Flog.warn(e);
            return false;
        }

        if (method.getStatusCode() >= 300){
            PersistentJson.removeWorkspace(f);
            return false;
        }
        return true;
    }
    static public HttpMethod getWorkspace(String owner, String workspace, FlooContext context) throws IOException {
        return apiRequest(new GetMethod(String.format("/api/workspace/%s/%s/", owner, workspace)), context);
    }

    static public HttpMethod apiRequest(HttpMethod method, FlooContext context) throws IOException, IllegalArgumentException{
        final HttpClient client = new HttpClient();
//        NOTE: we cant tell java to follow redirects because they can fail.
        HttpConnectionManager connectionManager = client.getHttpConnectionManager();
        HttpConnectionParams connectionParams = connectionManager.getParams();
        connectionParams.setParameter("http.protocol.handle-redirects", true);
        connectionParams.setSoTimeout(3000);
        connectionParams.setConnectionTimeout(3000);
        Settings settings = new Settings(context);
        HttpClientParams params = client.getParams();
        params.setAuthenticationPreemptive(true);
        client.setParams(params);
        Credentials credentials = new UsernamePasswordCredentials(settings.get("username"), settings.get("secret"));
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getHostConfiguration().setHost(Constants.defaultHost, 443, new Protocol("https", new SecureProtocolSocketFactory() {
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

    static public List<String> getOrgsCanAdmin(FlooContext context) {
        final GetMethod method = new GetMethod("/api/orgs/can/admin/");
        List<String> orgs = new ArrayList<String>();

        try {
            apiRequest(method, context);
        } catch (Exception e) {
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
        } catch (Exception e) {
            Flog.warn(e);
            context.error_message("Error getting Floobits organizations. Try again later or please contact support.");
        }

        return orgs;
    }
}

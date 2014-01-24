package floobits;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static floobits.Utils.createSSLContext;

class Request implements Serializable {
    public String owner;
    public String name;

    public Request (String owner, String name) {
        this.owner = owner;
        this.name = name;
    }
}

public class API {
    static public HttpMethod getWorkspace (String owner, String workspace) throws IOException {
        return apiRequest(new GetMethod(String.format("/api/workspace/%s/%s/", owner, workspace)));
    }

    static public HttpMethod createWorkspace (String owner, String workspace) throws IOException {
        final PostMethod method = new PostMethod("/api/workspace/");
        Gson gson = new Gson();
        String json = gson.toJson(new Request(owner, workspace));
        method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
        return apiRequest(method);
    }

    static public HttpMethod apiRequest (HttpMethod method) throws IOException, IllegalArgumentException{
        final HttpClient client = new HttpClient();
        HttpConnectionManager connectionManager = client.getHttpConnectionManager();
        HttpConnectionParams connectionParams = connectionManager.getParams();
        connectionParams.setSoTimeout(3000);
        connectionParams.setConnectionTimeout(3000);
        Settings settings = new Settings();
        client.getParams().setAuthenticationPreemptive(true);
        Credentials credentials = new UsernamePasswordCredentials(settings.get("username"), settings.get("secret"));
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getHostConfiguration().setHost(Shared.defaultHost, 443, new Protocol("https", new SecureProtocolSocketFactory() {
            @Override
            public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException, UnknownHostException {
                return createSSLContext().getSocketFactory().createSocket(socket, s, i, b);
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException, UnknownHostException {
                return createSSLContext().getSocketFactory().createSocket(s, i, inetAddress, i2);
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i2, HttpConnectionParams httpConnectionParams) throws IOException, UnknownHostException, ConnectTimeoutException {
                return createSSLContext().getSocketFactory().createSocket(s, i, inetAddress, i2);
            }

            @Override
            public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
                return createSSLContext().getSocketFactory().createSocket(s, i);
            }
        }, 443));
//        SSLContext sslContext = createSSLContext();
//        SSLContext.setDefault(sslContext);
        client.executeMethod(method);
        return method;
    }

    static public List<String> getOrgsCanAdmin () {
        final GetMethod method = new GetMethod("/api/orgs/can/admin/");
        List<String> orgs = new ArrayList<String>();

        try {
            apiRequest(method);
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
        }

        return orgs;
    }
}

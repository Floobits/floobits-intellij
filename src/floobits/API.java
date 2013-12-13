package floobits;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
        String url = String.format("https://%s/api/workspace/%s/%s/", Shared.defaultHost, owner, workspace);
        return apiRequest(new GetMethod(url));
    }

    static public HttpMethod createWorkspace (String owner, String workspace) throws IOException {
        final String url = String.format("https://%s/api/workspace/", Shared.defaultHost);
        final PostMethod method = new PostMethod(url);
        Gson gson = new Gson();
        String json = gson.toJson(new Request(owner, workspace));

        method.setRequestEntity(new StringRequestEntity(json, "application/json", "UTF-8"));
        return apiRequest(method);
    }

    static public HttpMethod apiRequest (HttpMethod method) throws IOException, IllegalArgumentException{
        final HttpClient client = new HttpClient();
//        client.setParams(new HttpClientParams());
        client.setTimeout(3000);
        client.setConnectionTimeout(3000);
        Settings settings = new Settings();
        client.getParams().setAuthenticationPreemptive(true);
        Credentials credentials = new UsernamePasswordCredentials(settings.get("username"), settings.get("secret"));
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.executeMethod(method);
        return method;
    }

    static public List<String> getOrgsCanAdmin () {
        final String url = String.format("https://%s/api/orgs/can/admin/", Shared.defaultHost);
        final GetMethod method = new GetMethod(url);
        List<String> orgs = new ArrayList<String>();

        try {
            apiRequest(method);
        } catch (Exception e) {
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
        } catch (Exception e) {
            Flog.error(e);
        }

        return orgs;
    }

// def get_basic_auth():
//     # TODO: use api_key if it exists
//     basic_auth = ('%s:%s' % (G.USERNAME, G.SECRET)).encode('utf-8')
//     basic_auth = base64.encodestring(basic_auth)
//     return basic_auth.decode('ascii').replace('\n', '')


// def apiRequest(url, data=None):
//     if data:
//         data = json.dumps(data).encode('utf-8')
//     r = Request(url, data=data)
//     r.add_header('Authorization', 'Basic %s' % get_basic_auth())
//     r.add_header('Accept', 'application/json')
//     r.add_header('Content-type', 'application/json')
//     r.add_header('User-Agent', 'Floobits Plugin %s %s %s py-%s.%s' % (editor.name(), G.__PLUGIN_VERSION__, editor.platform(), sys.version_info[0], sys.version_info[1]))
//     return urlopen(r, timeout=5)


// def create_workspace(post_data):
//     url = 'https://%s/api/workspace/' % G.DEFAULT_HOST
//     return apiRequest(url, post_data)


// def get_workspace_by_url(url):
//     result = utils.parse_url(url)
//     api_url = 'https://%s/api/workspace/%s/%s/' % (result['host'], result['owner'], result['workspace'])
//     return apiRequest(api_url)


// def get_workspace(owner, workspace):
//     api_url = 'https://%s/api/workspace/%s/%s/' % (G.DEFAULT_HOST, owner, workspace)
//     return apiRequest(api_url)


// def get_workspaces():
//     api_url = 'https://%s/api/workspace/can/view/' % (G.DEFAULT_HOST)
//     return apiRequest(api_url)


// def get_orgs():
//     api_url = 'https://%s/api/orgs/' % (G.DEFAULT_HOST)
//     return apiRequest(api_url)


// def get_orgs_can_admin():
//     api_url = 'https://%s/api/orgs/can/admin/' % (G.DEFAULT_HOST)
//     return apiRequest(api_url)


// def send_error(data):
//     try:
//         api_url = 'https://%s/api/error/' % (G.DEFAULT_HOST)
//         return apiRequest(api_url, data)
//     except Exception as e:
//         print(e)
//     return None

}

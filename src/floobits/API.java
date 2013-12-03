package floobits;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;


import java.io.IOException;
import java.net.UnknownHostException;

// https://floobits.com/awreece/timothy-interview
public class API {
    static public void getWorkspace(String owner, String workspace) {
		String url = String.format("https://%s/api/workspace/%s/%s/", Shared.defaultHost, owner, workspace);
        final GetMethod method = new GetMethod(url);
        try{
            apiRequest(method, url);
        } catch (Exception e) {
            Flog.error(e);
        }
	}
    static public void createWorkspace(String owner, String workspace) {
        final String url = String.format("https://%s/api/workspace/%s/%s/", Shared.defaultHost, owner, workspace);
        final PostMethod method = new PostMethod(url);
        try{
            apiRequest(method, url);
        } catch (Exception e) {
            Flog.error(e);
        }
    }

	static public void apiRequest(HttpMethod method, String url) throws Exception{
		final HttpClient client = new HttpClient();
		client.setTimeout(3000);
		client.setConnectionTimeout(3000);
		try {
            Settings settings = new Settings();
            Credentials credentials = new UsernamePasswordCredentials(settings.get("usernmae"), settings.get("secret"));
            client.getState().setCredentials(AuthScope.ANY, credentials);
            final int code = client.executeMethod(method);
            switch (code) {
                case HttpStatus.SC_OK:
                case HttpStatus.SC_CREATED:
                case HttpStatus.SC_ACCEPTED:
                case HttpStatus.SC_NO_CONTENT:
                    return;
                case HttpStatus.SC_BAD_REQUEST:
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_PAYMENT_REQUIRED:
                case HttpStatus.SC_FORBIDDEN:
                    // String message = getErrorMessage(method);
                    // if (message.contains("API rate limit exceeded")) {
                    //   throw new GithubRateLimitExceededException(message);
                    // }
                    throw new Exception("Request response: ");
                default:
                    throw new Exception("Thing");
            }
		} catch (UnknownHostException e) {
		 Flog.error(e);
		 return;
		}
		catch (IOException e) {
		 Flog.error(e);
		 return;
		}
		catch (IllegalArgumentException e) {
		 Flog.error(e);
		 return;
		}
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
package floobits;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.net.UnknownHostException;

package org.apache.http.examples.client;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * A simple example that uses HttpClient to execute an HTTP request against
 * a target site that requires user authentication.
 */
public class ClientAuthentication {

    public static void main(String[] args) throws Exception {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope("localhost", 443),
                new UsernamePasswordCredentials("username", "password"));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();
        try {
            HttpGet httpget = new HttpGet("https://localhost/protected");

            System.out.println("executing request" + httpget.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {
                HttpEntity entity = response.getEntity();

                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                if (entity != null) {
                    System.out.println("Response content length: " + entity.getContentLength());
                }
                EntityUtils.consume(entity);
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }
}

public class API {
    static public void getWorkspace(String owner, String workspace) {
		String url = String.format("https://%s/api/workspace/%s/%s/", (Shared.defaultHost, owner, workspace));
		apiRequest(url);
	}

	static public void apiRequest(String url) {
		final HttpClient client = new HttpClient();
		client.setTimeout(3000);
		client.setConnectionTimeout(3000);
		// see http://hc.apache.org/httpclient-3.x/cookies.html
		client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		try {
		 final GetMethod method = new GetMethod(url);
		 final int code = client.executeMethod(method);
         client.setParams(new HttpClientParams());

		 return code == HttpStatus.SC_OK || code == HttpStatus.SC_REQUEST_TIMEOUT
		        ? MyFetchResult.OK 
		        : MyFetchResult.NONEXISTENCE;
		}
		catch (UnknownHostException e) {
		 LOG.info(e);
		 return MyFetchResult.UNKNOWN_HOST;
		}
		catch (IOException e) {
		 LOG.info(e);
		 return MyFetchResult.OK;
		}
		catch (IllegalArgumentException e) {
		 LOG.debug(e);
		 return MyFetchResult.OK;
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
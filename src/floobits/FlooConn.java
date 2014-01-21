package floobits;

import java.io.*;
import java.net.SocketTimeoutException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import javax.net.ssl.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FlooConn extends Thread {
    private String cert =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIHyTCCBbGgAwIBAgIBATANBgkqhkiG9w0BAQUFADB9MQswCQYDVQQGEwJJTDEW\n" +
        "MBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0YWwg\n" +
        "Q2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3RhcnRDb20gQ2VydGlmaWNh\n" +
        "dGlvbiBBdXRob3JpdHkwHhcNMDYwOTE3MTk0NjM2WhcNMzYwOTE3MTk0NjM2WjB9\n" +
        "MQswCQYDVQQGEwJJTDEWMBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMi\n" +
        "U2VjdXJlIERpZ2l0YWwgQ2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3Rh\n" +
        "cnRDb20gQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwggIiMA0GCSqGSIb3DQEBAQUA\n" +
        "A4ICDwAwggIKAoICAQDBiNsJvGxGfHiflXu1M5DycmLWwTYgIiRezul38kMKogZk\n" +
        "pMyONvg45iPwbm2xPN1yo4UcodM9tDMr0y+v/uqwQVlntsQGfQqedIXWeUyAN3rf\n" +
        "OQVSWff0G0ZDpNKFhdLDcfN1YjS6LIp/Ho/u7TTQEceWzVI9ujPW3U3eCztKS5/C\n" +
        "Ji/6tRYccjV3yjxd5srhJosaNnZcAdt0FCX+7bWgiA/deMotHweXMAEtcnn6RtYT\n" +
        "Kqi5pquDSR3l8u/d5AGOGAqPY1MWhWKpDhk6zLVmpsJrdAfkK+F2PrRt2PZE4XNi\n" +
        "HzvEvqBTViVsUQn3qqvKv3b9bZvzndu/PWa8DFaqr5hIlTpL36dYUNk4dalb6kMM\n" +
        "Av+Z6+hsTXBbKWWc3apdzK8BMewM69KN6Oqce+Zu9ydmDBpI125C4z/eIT574Q1w\n" +
        "+2OqqGwaVLRcJXrJosmLFqa7LH4XXgVNWG4SHQHuEhANxjJ/GP/89PrNbpHoNkm+\n" +
        "Gkhpi8KWTRoSsmkXwQqQ1vp5Iki/untp+HDH+no32NgN0nZPV/+Qt+OR0t3vwmC3\n" +
        "Zzrd/qqc8NSLf3Iizsafl7b4r4qgEKjZ+xjGtrVcUjyJthkqcwEKDwOzEmDyei+B\n" +
        "26Nu/yYwl/WL3YlXtq09s68rxbd2AvCl1iuahhQqcvbjM4xdCUsT37uMdBNSSwID\n" +
        "AQABo4ICUjCCAk4wDAYDVR0TBAUwAwEB/zALBgNVHQ8EBAMCAa4wHQYDVR0OBBYE\n" +
        "FE4L7xqkQFulF2mHMMo0aEPQQa7yMGQGA1UdHwRdMFswLKAqoCiGJmh0dHA6Ly9j\n" +
        "ZXJ0LnN0YXJ0Y29tLm9yZy9zZnNjYS1jcmwuY3JsMCugKaAnhiVodHRwOi8vY3Js\n" +
        "LnN0YXJ0Y29tLm9yZy9zZnNjYS1jcmwuY3JsMIIBXQYDVR0gBIIBVDCCAVAwggFM\n" +
        "BgsrBgEEAYG1NwEBATCCATswLwYIKwYBBQUHAgEWI2h0dHA6Ly9jZXJ0LnN0YXJ0\n" +
        "Y29tLm9yZy9wb2xpY3kucGRmMDUGCCsGAQUFBwIBFilodHRwOi8vY2VydC5zdGFy\n" +
        "dGNvbS5vcmcvaW50ZXJtZWRpYXRlLnBkZjCB0AYIKwYBBQUHAgIwgcMwJxYgU3Rh\n" +
        "cnQgQ29tbWVyY2lhbCAoU3RhcnRDb20pIEx0ZC4wAwIBARqBl0xpbWl0ZWQgTGlh\n" +
        "YmlsaXR5LCByZWFkIHRoZSBzZWN0aW9uICpMZWdhbCBMaW1pdGF0aW9ucyogb2Yg\n" +
        "dGhlIFN0YXJ0Q29tIENlcnRpZmljYXRpb24gQXV0aG9yaXR5IFBvbGljeSBhdmFp\n" +
        "bGFibGUgYXQgaHR0cDovL2NlcnQuc3RhcnRjb20ub3JnL3BvbGljeS5wZGYwEQYJ\n" +
        "YIZIAYb4QgEBBAQDAgAHMDgGCWCGSAGG+EIBDQQrFilTdGFydENvbSBGcmVlIFNT\n" +
        "TCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTANBgkqhkiG9w0BAQUFAAOCAgEAFmyZ\n" +
        "9GYMNPXQhV59CuzaEE44HF7fpiUFS5Eyweg78T3dRAlbB0mKKctmArexmvclmAk8\n" +
        "jhvh3TaHK0u7aNM5Zj2gJsfyOZEdUauCe37Vzlrk4gNXcGmXCPleWKYK34wGmkUW\n" +
        "FjgKXlf2Ysd6AgXmvB618p70qSmD+LIU424oh0TDkBreOKk8rENNZEXO3SipXPJz\n" +
        "ewT4F+irsfMuXGRuczE6Eri8sxHkfY+BUZo7jYn0TZNmezwD7dOaHZrzZVD1oNB1\n" +
        "ny+v8OqCQ5j4aZyJecRDjkZy42Q2Eq/3JR44iZB3fsNrarnDy0RLrHiQi+fHLB5L\n" +
        "EUTINFInzQpdn4XBidUaePKVEFMy3YCEZnXZtWgo+2EuvoSoOMCZEoalHmdkrQYu\n" +
        "L6lwhceWD3yJZfWOQ1QOq92lgDmUYMA0yZZwLKMS9R9Ie70cfmu3nZD0Ijuu+Pwq\n" +
        "yvqCUqDvr0tVk+vBtfAii6w0TiYiBKGHLHVKt+V9E9e4DGTANtLJL4YSjCMJwRuC\n" +
        "O3NJo2pXh5Tl1njFmUNj403gdy3hZZlyaQQaRwnmDwFWJPsfvw55qVguucQJAX6V\n" +
        "um0ABj6y6koQOdjQK/W/7HW/lwLFCRsI3FU34oH7N4RDYiDK51ZLZer+bMEkkySh\n" +
        "NOsF/5oirpt9P/FlUQqmMGqz9IgcgA38corog14=\n" +
        "-----END CERTIFICATE-----";
    protected Writer out;
    protected BufferedReader in;
    protected SSLSocket socket;
    protected FlooHandler handler;
    Boolean connected = false;

    private Integer MAX_RETRIES = 20;
    private Integer SOCKET_TIMEOUT = 0; // Inifinity right now, which is default.
    private Integer INITIAL_RECONNECT_DELAY = 500;
    protected Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;

    public FlooConn(FlooHandler handler) {
        this.handler = handler;
    }

    public void write (Serializable obj) {
        String data = new Gson().toJson(obj);
        try {
            this.out.write(data + "\n");
            this.out.flush();
        } catch (Exception e) {
            if (retries > -1) Flog.warn(e);
        }
    }

    public void run () {
        connect();
    }

    public Boolean shutDown() {
        if (!connected) {
            // Not connectedd.
            return false;
        }
        retries = -1;
        cleanUp();
        return true;
    }

    protected void cleanUp() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null)  {
                socket.shutdownOutput();
                socket.shutdownOutput();
                socket.close();
            }
        } catch (Exception ignored) {}
        connected = false;
    }

    protected void handle (String line) {
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        if (name == null) {
            Flog.warn("No name for request, ignoring");
            return;
        }
        String name1 = name.getAsString();
        try {
            this.handler.on_data(name1, obj);
        } catch (Exception e) {
            Flog.warn(e);
            if (name1.equals("room_info")) {
                shutDown();
            }
        }
    }

    protected void reconnect() {
        Flog.info("reconnecting");
        if (!connected) {
            Flog.info("Aborting reconnect, we do not want to be connected.");
            return;
        }
        cleanUp();
        retries -= 1;
        if (retries <= 0) {
            Flog.warn("I give up connecting.");
            FloobitsPlugin.flooHandler.shut_down();
            return;
        }
        delay = Math.min(10000, Math.round((float)1.5 * delay));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Flog.warn(e);
        }
        connect();
    }

    protected void connect() {
        X509TrustManager x509TrustManager = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                InputStream is = new ByteArrayInputStream(cert.getBytes());
                CertificateFactory cf;
                X509Certificate cert;

                cf = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) cf.generateCertificate(is);
                cert.checkValidity();

                ArrayList<X509Certificate> x509Certificates = new ArrayList<X509Certificate>(Arrays.asList(certs));
                x509Certificates.add(cert);
                X509Certificate a, b;
                a = x509Certificates.get(0);
                for (int i = 1; i < x509Certificates.size(); i++) {
                    b = x509Certificates.get(i);
                    a.checkValidity();
                    try {
                        a.verify(b.getPublicKey());
                    } catch (Exception e) {
                        Flog.warn(e);
                        throw new CertificateException(e);
                    }
                    a = b;
                }
            }
        };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new javax.net.ssl.TrustManager[]{x509TrustManager}, new SecureRandom());
        } catch (Exception e) {
            Flog.warn(e);
            return;
        }
        FlooUrl flooUrl = handler.getUrl();
        try{
            socket = (SSLSocket) sc.getSocketFactory().createSocket(flooUrl.host, flooUrl.port);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
        } catch (IOException e) {
            Flog.warn("Error connecting %s", e);
            reconnect();
            return;
        }
        try {
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            this.handler.on_connect();
            connected = true;
            retries = MAX_RETRIES;
            delay = INITIAL_RECONNECT_DELAY;

            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        if (retries != -1) Flog.warn("socket died");
                        break;
                    }
                    this.handle(line);
                } catch (SocketTimeoutException e) {
                    Flog.info("Caught timeout on socket. %s", socket.isClosed());
                    if (socket.isClosed()) {
                        reconnect();
                        return;
                    }
                } catch (IOException e) {
                    if (retries != -1) Flog.warn(e);
                    break;
                }
            }
        } catch (Exception e) {
            if (retries != -1) Flog.warn(e);
        } finally {
            reconnect();
        }
    }
}

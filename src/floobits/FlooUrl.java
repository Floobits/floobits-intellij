package floobits;

import java.net.*;


class FlooUrl {
    public String proto;
    public String host;
    public String owner;
    public String workspace;

    public Integer port;
    public boolean secure;

    public FlooUrl(String url) throws MalformedURLException {
        URL u = new URL(url);
        String path = u.getPath();
        String[] parts = path.split("/");

        this.host = u.getHost();
        this.owner = parts[1];
        this.workspace = parts[2];
        if (this.owner.equals("r")) {
            this.owner = parts[2];
            this.workspace = parts[3];
        }
        this.port = u.getPort();
        this.proto = u.getProtocol();

        if (this.proto.equals("http")) {
            this.secure = false;
        } else {
            this.secure = true;
        }

        if (this.port < 0) {
            if (this.secure) {
                this.port = 3448;
            } else {
                this.port = 3148;
            }
        }
    }

    public FlooUrl(String host, String owner, String workspace, Integer port, boolean secure) {
        this.host = host;
        this.owner = owner;
        this.workspace = workspace;
        this.port = port < 0 ? 3448 : port;
        this.secure = secure;
        this.proto = secure ? "https" : "http";
    }

    public String toString() {
        String port = "";

        if (this.secure) {
            proto = "https";
            if (this.port != 3448) {
                port = String.format(":%s", this.port);
            }
        } else {
            proto = "http";
            if (this.port != 3148) {
                port = String.format(":%s", this.port);
            }
        }
        return String.format("%s://%s%s/%s/%s/", proto, this.host, port, this.owner, this.workspace);
    }
}

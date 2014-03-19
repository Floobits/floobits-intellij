package floobits.common;

import java.io.Serializable;

public class Workspace implements Serializable {
    public String url;
    public String path;

    Workspace () {

    }

    Workspace(String url, String path) {
        this.url = url;
        this.path = path;
    }

    public void clean() {
      if (!this.url.endsWith("/")) {
          this.url += "/";
      }
    }
}

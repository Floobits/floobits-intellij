package floobits.common;

public enum Encoding {
    BASE64("base64"), UTF8("utf8");
    private final String enc;

    Encoding (String enc) {
        this.enc = enc;
    }

    @Override
    public String toString () {
        return this.enc;
    }

    public static Encoding from (String str) {
        for(Encoding v : Encoding.values()) {
            if(v.toString().equals(str)) {
                return v;
            }
        }
        return null;
    }
}

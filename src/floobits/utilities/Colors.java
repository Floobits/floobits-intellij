package floobits.utilities;

import com.intellij.ui.JBColor;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;

public class Colors {
    static protected int alpha =  (int)(0.4f * 255);
    static protected JBColor[] colors = new JBColor[] {
        new JBColor(new Color(191,255,0,alpha),new Color(191,255,0,alpha)),
        new JBColor(new Color(0,0,0,alpha),new Color(0,0,0,alpha)),
        new JBColor(new Color(0,0,255,alpha),new Color(0,0,255,alpha)),
        new JBColor(new Color(0,0,139,alpha),new Color(0,0,139,alpha)),
        new JBColor(new Color(255,0,255,alpha),new Color(255,0,255,alpha)),
        new JBColor(new Color(191,191,191,alpha),new Color(191,191,191,alpha)),
        new JBColor(new Color(0,128,0,alpha),new Color(0,128,0,alpha)),
        new JBColor(new Color(173,255,47,alpha),new Color(173,255,47,alpha)),
        new JBColor(new Color(75,0,130,alpha),new Color(75,0,130, alpha)),
        new JBColor(new Color(255,0,255,alpha),new Color(255,0,255,alpha)),
        new JBColor(new Color(25,25,112,alpha),new Color(25,25,112,alpha)),
        new JBColor(new Color(128,0,0,alpha),new Color(128,0,0,alpha)),
        new JBColor(new Color(255,165,0,alpha),new Color(255,165,0,alpha)),
        new JBColor(new Color(255,69,0,alpha),new Color(255,69,0,alpha)),
        new JBColor(new Color(128,0,128,alpha),new Color(128,0,128 ,alpha)),
        new JBColor(new Color(255,0,0,alpha), new Color(255,0,0,alpha)),
        new JBColor(new Color(0,128,128,alpha),new Color(0,128,128,alpha)),
        new JBColor(new Color(255,255,0,alpha),new Color(255,255,0,alpha)),
    };

    public static JBColor getFGColor() {
        return new JBColor(new Color(255,255,255,255),new Color(255,255,255,255));
    }

    // http://sny.no/2011/11/java-hex
    public static String getHex(JBColor color) {
        return toHex(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String toHex(int r, int g, int b) {
        return "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b);
    }

    private static String toBrowserHexValue(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

    public static JBColor getColorForUser(String username) {
        int i = 0;
        for(char c : DigestUtils.md5Hex(username).toCharArray()) {
            i += (int)c;
        }
        return colors[i % colors.length];
    }
}

package floobits.utilities;

import com.intellij.ui.JBColor;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;

public class Colors {
    static protected JBColor[] colors = new JBColor[] {
        new JBColor(new Color(88,94,73),new Color(88,94,73)),
        new JBColor(new Color(0,0,0),new Color(0,0,0)),
        new JBColor(new Color(0,0,255),new Color(0,0,255)),
        new JBColor(new Color(0,0,139),new Color(0,0,139)),
        new JBColor(new Color(186,38,64),new Color(186,38,64)),
        new JBColor(new Color(152,144,155),new Color(152,144,155)),
        new JBColor(new Color(0,128,0),new Color(0,128,0)),
        new JBColor(new Color(133,135,26),new Color(133,135,26)),
        new JBColor(new Color(75,0,130),new Color(75,0,130)),
        new JBColor(new Color(109,34,67),new Color(109,34,67)),
        new JBColor(new Color(25,25,112),new Color(25,25,112)),
        new JBColor(new Color(128,0,0),new Color(128,0,0)),
        new JBColor(new Color(247,135,26),new Color(247,135,26)),
        new JBColor(new Color(255,69,0),new Color(255,69,0)),
        new JBColor(new Color(128,0,128),new Color(128,0,128 )),
        new JBColor(new Color(255,0,0), new Color(255,0,0)),
        new JBColor(new Color(0,128,128),new Color(0,128,128)),
        new JBColor(new Color(252,219,116),new Color(252,219, 116)),
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

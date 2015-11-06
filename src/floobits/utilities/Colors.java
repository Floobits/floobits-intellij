package floobits.utilities;

import com.intellij.ui.JBColor;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Colors {
    final static protected ArrayList<String> HIGHLIGHT_COLORS = new ArrayList<String>(){{
        add("lime");
        add("black");
        add("blue");
        add("darkblue");
        add("fuchsia");
        add("gray");
        add("green");
        add("greenyellow");
        add("indigo");
        add("magenta");
        add("midnightblue");
        add("maroon");
        add("orange");
        add("orangered");
        add("purple");
        add("red");
        add("teal");
        add("yellow");
    }};

    static protected JBColor[] colors = new JBColor[] {
        new JBColor(new Color(0, 255, 0, 77), new Color(0, 255, 0, 77)),
        new JBColor(new Color(0, 0, 0, 77), new Color(0, 0, 0, 77)),
        new JBColor(new Color(0, 0, 255, 77), new Color(0, 0, 255, 77)),
        new JBColor(new Color(0, 0, 139, 77), new Color(0, 0, 139, 77)),
        new JBColor(new Color(193, 84, 193, 77), new Color(193, 84, 193, 77)),
        new JBColor(new Color(128, 128, 128, 77), new Color(128, 128, 128, 77)),
        new JBColor(new Color(0, 128, 0, 77), new Color(0, 128, 0, 77)),
        new JBColor(new Color(173, 255, 47, 77), new Color(173, 255, 47, 77)),
        new JBColor(new Color(75, 0, 130, 77), new Color(75, 0, 130, 77)),
        new JBColor(new Color(255, 0, 255, 77), new Color(255, 0, 255, 77)),
        new JBColor(new Color(25, 25, 112, 77), new Color(25, 25, 112, 77)),
        new JBColor(new Color(128, 0, 0, 77), new Color(128, 0, 0, 77)),
        new JBColor(new Color(255, 165, 0, 77), new Color(255, 165, 0, 77)),
        new JBColor(new Color(255, 69, 0, 77), new Color(255, 69, 0, 77)),
        new JBColor(new Color(128, 0, 128, 77), new Color(128, 0, 128, 77)),
        new JBColor(new Color(255, 0, 0, 77), new Color(255, 0, 0, 77)),
        new JBColor(new Color(0, 128, 128, 77), new Color(0, 128, 128, 77)),
        new JBColor(new Color(255, 255, 0, 77), new Color(255, 255, 0, 77))
    };

    public static JBColor getFGColor() {
        return new JBColor(new Color(255, 255, 255, 255),new Color(255, 255, 255, 255));
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
        String color = color_map.get(username);
        if (color != null) {
            int indexOf = HIGHLIGHT_COLORS.indexOf(color);
            if (indexOf >= 0) {
                return colors[indexOf];
            }
        }

        int i = 0;
        for(char c : DigestUtils.md5Hex(username).toCharArray()) {
            i += (int)c;
        }
        return colors[i % colors.length];
    }

    public static final HashMap<String, String> color_map = new HashMap<String, String>();
}

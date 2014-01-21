package floobits;

import com.intellij.ui.JBColor;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;

public class Colors {
    static protected int alpha =  (int)(0.8f * 255);
    static protected JBColor[] colors = new JBColor[] {
        new JBColor(new Color(191,255,0,alpha), new Color(191,255,0)),
        new JBColor(new Color(0,0,0,alpha), new Color(0,0,0,alpha)),
        JBColor.BLUE,
        new JBColor(new Color(0,0,139),new Color(0,0,139,alpha)),
        new JBColor(new Color(255,0,255,alpha),new Color(255,0,255)),
        JBColor.gray,
        new JBColor(new Color(0,128,0,alpha),new Color(0,128,0,alpha)),
        new JBColor(new Color(173,255,47),new Color(173,255,47)),
        new JBColor(new Color(75,0,130),new Color(75,0,130, alpha)),
        new JBColor(new Color(255,0,255),new Color(255,0,255)),
        new JBColor(new Color(25,25,112),new Color(25,25,112,alpha)),
        new JBColor(new Color(128,0,0),new Color(128,0,0,alpha)),
        new JBColor(new Color(255,165,0),new Color(255,165,0)),
        new JBColor(new Color(255,69,0),new Color(255,69,0 )),
        new JBColor(new Color(128,0,128),new Color(128,0,128 ,alpha)),
        new JBColor(JBColor.red, new Color(255,0,0,alpha)),
        new JBColor(new Color(255,165,0),new Color(255,165,0)),
        new JBColor(new Color(0,128,128),new Color(0,128,128)),
        JBColor.yellow
    };

    static JBColor getColorForUser(String username) {
        int i = 0;
        for(char c : DigestUtils.md5Hex(username).toCharArray()) {
            i += (int)c;
        }
        return colors[i % colors.length];
    };
}

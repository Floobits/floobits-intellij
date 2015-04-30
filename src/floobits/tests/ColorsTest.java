package floobits.tests;

import com.intellij.ui.JBColor;
import floobits.utilities.Colors;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class ColorsTest {

    @Test
    public void testGetFGColor(){
        JBColor expected = new JBColor(new Color(255, 255, 255, 255), new Color(255, 255, 255, 255));
        JBColor received = Colors.getFGColor();
        assertEquals("FG color should be black.", expected.getRGB(), received.getRGB());
    }

    @Test
    public void testGetHex() {
        JBColor c = new JBColor(new Color(255, 0, 0), new Color(255, 0, 0));
        assertEquals("Should have gotten red hex value", "#FF0000", Colors.getHex(c));
    }

    @Test
    public void testToHex() {
        assertEquals("Should have gotten red hex value", "#FF0000", Colors.toHex(255, 0, 0));
    }

    @Test
    public void testGetColorForUser() {
        assertNotNull("testing get color for username", Colors.getColorForUser("bjorn"));
    }
}

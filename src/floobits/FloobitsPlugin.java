package floobits;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class FloobitsPlugin implements ApplicationComponent, PersistentStateComponent<Element> {
    public FloobitsPlugin() {
    }

    @Override
    public void initComponent() {
    }

    public static void joinWorkspace(String url) {
        try {
            FlooUrl f = new FlooUrl(url);
            FlooHandler handler = new FlooHandler(f);
        } catch (Exception e) {
            Flog.error(e);
        }
    }

    public static void shareProject(Project p) {
        try {
            FlooHandler handler = new FlooHandler(p);
        } catch (Exception e) {
            Flog.error(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return super.toString();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int hashCode() {
        return super.hashCode();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void disposeComponent() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "floobits";
    }

    @Nullable
    @Override
    public Element getState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void loadState(Element element) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

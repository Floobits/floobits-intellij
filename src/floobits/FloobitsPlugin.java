package floobits;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentListener;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import org.jdom.Element;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dmp.diff_match_patch;
import floobits.FlooConn;

public class FloobitsPlugin implements ApplicationComponent, PersistentStateComponent<Element> {
    private static Logger Log = Logger.getInstance(FloobitsPlugin.class);
    public FloobitsPlugin() {

    }

    @Override
    public void initComponent() {
//        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
//
//        MessageBusConnection connection = bus.connect();

//        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
//                new FileDocumentManagerAdapter() {
//                    @Override
//                    public void beforeDocumentSaving(Document document) {
//                        // create your custom logic here
//                    }
//                });
//        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener);
    }

    public static void joinWorkspace() {
        Log.info("Joining workspace... NOT");
        new FlooConn("kansface", "asdf");
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

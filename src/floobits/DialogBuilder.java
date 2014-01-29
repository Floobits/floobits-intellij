package floobits;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DialogBuilder extends DialogWrapper {
    private JPanel jPanel;
    private RunLater<Boolean> runLater;

    public static void build(final String title, final String body, final RunLater<Boolean> runLater) {
        JLabel jLabel = new JLabel();
        jLabel.setText(String.format("<html><p>%s</p></html>", body));
        build(title, jLabel, runLater);
    }

    public static void build(final String title, final JComponent component, final RunLater<Boolean> runLater) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                DialogBuilder dialogBuilder = new DialogBuilder(title, component, runLater);
                dialogBuilder.createCenterPanel();
                dialogBuilder.show();
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    protected DialogBuilder(String title, final JComponent component, RunLater<Boolean> runLater) {
        super(true);
        jPanel = new JPanel();
        this.runLater = runLater;
        init();
        this.setTitle(title);
        jPanel.add(component);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        runLater.run(false);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        runLater.run(true);
    }
}

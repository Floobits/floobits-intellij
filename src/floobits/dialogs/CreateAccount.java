package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CreateAccount extends DialogWrapper {
    private JPanel jPanel;
    protected Project project;


    private class CreateAccountAction extends DialogWrapper.DialogWrapperAction {

        protected CreateAccountAction() {
            super("Create a Floobits account");
        }

        @Override
        protected void doAction(ActionEvent e) {
            Flog.info("Creating account from button press");
            doOKAction();
        }
    }


    private class LinkAccountAction extends DialogWrapper.DialogWrapperAction {

        protected LinkAccountAction() {
            super("I already have an account");
        }

        @Override
        protected void doAction(ActionEvent e) {
            Flog.info("Linking account from button press");
            doCancelAction();
        }
    }


    public CreateAccount(Project project) {
        super(project, true);
        this.project = project;
        jPanel = new JPanel();
        init();
        this.setTitle("No Floobits Account Detected");
        JLabel label = new JLabel("You need a Floobits account! If you don't have one we will create one for you.");
        jPanel.add(label);
    }

    @Override
    public void createDefaultActions () {
        super.createDefaultActions();
        myOKAction = new CreateAccountAction();
        myCancelAction = new LinkAccountAction();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return jPanel;
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        FlooContext context;
        if (project == null) {
            context = new FlooContext(null);
        } else {
            context = FloobitsPlugin.getInstance(project).context;
        }
        context.linkEditor();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        FlooContext context;
        if (project == null) {
            context = new FlooContext(null);
        } else {
            context = FloobitsPlugin.getInstance(project).context;
        }
        context.createAccount();
    }
}


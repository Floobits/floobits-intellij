package floobits.dialogs;

import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.interfaces.IContext;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CreateAccount extends CustomButtonDialogWrapper {
    private JPanel jPanel;
    public CreateAccount(final Project project, final Runnable afterSetup) {
        super(project, true);
        jPanel = new JPanel();
        this.setTitle("No Floobits Account Detected");
        JLabel label = new JLabel("You need a Floobits account! If you don't have one we will create one for you.");
        jPanel.add(label);
        CustomButtonAction cancelAction = new CustomButtonAction("Cancel", new Runnable() {
            @Override
            public void run() {
                doCancelAction();
            }
        });
        CustomButtonAction linkAccountAction = new CustomButtonAction("I already have an account", new Runnable() {
            @Override
            public void run() {
                Flog.info("Linking account from button press");
                IContext context;
                if (project == null) {
                    context = new ContextImpl(null);
                } else {
                    FloobitsPlugin floobitsPlugin = project.getService(FloobitsPlugin.class);
                    context = floobitsPlugin.context;
                }
                context.linkEditor(afterSetup);
            }
        });
        CustomButtonAction createAccountAction = new CustomButtonAction("Create a Floobits account", new Runnable() {
            @Override
            public void run() {
                Flog.info("Creating account from button press");
                IContext context;
                if (project == null) {
                    context = new ContextImpl(null);
                } else {
                    FloobitsPlugin floobitsPlugin = project.getService(FloobitsPlugin.class);
                    context = floobitsPlugin.context;
                }
                context.createAccount(afterSetup);
            }
        });
        actions = new Action[]{cancelAction, linkAccountAction, createAccountAction};
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return jPanel;
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}


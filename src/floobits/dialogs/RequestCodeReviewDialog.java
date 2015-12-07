package floobits.dialogs;

import com.intellij.openapi.project.Project;
import floobits.common.API;
import floobits.common.FlooUrl;
import floobits.common.interfaces.IContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;

public class RequestCodeReviewDialog extends CustomButtonDialogWrapper {
    protected JPanel container;

    public RequestCodeReviewDialog(final FlooUrl flooUrl, final IContext context, Project project) {
        super(project, true);
        this.setTitle("Request Code Review");
        container = new JPanel();
        container.setLayout(new BorderLayout());
        JLabel infoLabel = new JLabel("Please describe the problem.  A human will send you an email.");
        final JTextArea description = new JTextArea();
        description.setRows(10);
        JScrollPane scrollPane = new JScrollPane(description);
        container.add(infoLabel, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.SOUTH);

        CustomButtonAction cancel = new CustomButtonAction("Cancel", new Runnable() {
            @Override
            public void run() {
                container.setVisible(false);
            }
        });
        CustomButtonAction requestReviewAction = new CustomButtonAction("Request Review", new Runnable() {
            @Override
            public void run() {
                container.setVisible(false);
                String text = description.getText();
                String s = API.requestReview(flooUrl, text, context);
                context.flashMessage(s);
            }
        });
        actions = new Action[]{requestReviewAction, cancel};
        description.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent ancestorEvent) {
                JComponent component = ancestorEvent.getComponent();
                component.requestFocusInWindow();
            }

            @Override
            public void ancestorRemoved(AncestorEvent ancestorEvent) {

            }

            @Override
            public void ancestorMoved(AncestorEvent ancestorEvent) {

            }
        });
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return container;
    }
}
package floobits.dialogs;

import com.intellij.openapi.project.Project;
import floobits.common.API;
import floobits.common.FlooUrl;
import floobits.common.interfaces.IContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
        scrollPane.requestFocus();


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
                API.requestReview(flooUrl, text, context);
            }
        });
        actions = new Action[]{requestReviewAction, cancel};
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return container;
    }
}
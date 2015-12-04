package floobits.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class RequestCodeReviewDialog extends CustomButtonDialogWrapper {
    protected JPanel container;

    public RequestCodeReviewDialog(Project project) {
        super(project, true);
        this.setTitle("Request Code Review");
        container = new JPanel();
        container.setLayout(new BorderLayout());
        JLabel infoLabel = new JLabel("Please describe the problem.  A human will send you an email.");
        JTextArea description = new JTextArea();
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
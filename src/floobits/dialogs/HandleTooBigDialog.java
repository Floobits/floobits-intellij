package floobits.dialogs;

import floobits.common.Ignore;
import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;


public class HandleTooBigDialog extends CustomButtonDialogWrapper {
    protected FileListPromptForm form;

    public HandleTooBigDialog(final RunLater<Boolean> runLater, final LinkedList<Ignore> bigStuff) {
        super(true);
        setTitle("Your Project Is Too Large");
        form = new FileListPromptForm(String.format("Ignore the following (%d) directories and continue?", bigStuff.size()));
        ArrayList<String> problems = new ArrayList<String>();
        for (Ignore ignore : bigStuff) {
            String size = NumberFormat.getInstance().format(ignore.size/1000);
            problems.add(String.format("<html><p>%s <i>(%s MB)</i></p></html>", ignore.file.getPath(), size));
        }
        form.setItems(problems.toArray(new String[problems.size()]));
        CustomButtonAction cancelAction = new CustomButtonAction("Quit", new Runnable() {
            @Override
            public void run() {
                runLater.run(false);
            }
        });
        CustomButtonAction okAction = new CustomButtonAction("Ignore and Continue", new Runnable() {
            @Override
            public void run() {
                runLater.run(true);
            }
        });
        actions = new Action[]{cancelAction, okAction};
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();

    }
}

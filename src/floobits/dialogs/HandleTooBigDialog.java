package floobits.dialogs;

import floobits.common.Ignore;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class HandleTooBigDialog extends CustomButtonDialogWrapper {
    protected FileListPromptForm form;

    public HandleTooBigDialog(final HashMap<String, Integer> bigStuff) {
        super(true);
        setTitle("Your Project Is Too Large");
        form = new FileListPromptForm(String.format("Ignore the following (%d) directories and continue?", bigStuff.size()));
        ArrayList<String> problems = new ArrayList<String>();
        for (Map.Entry<String, Integer> bigData : bigStuff.entrySet()) {
                String size = NumberFormat.getInstance().format(bigData.getValue()/1000);
            problems.add(String.format("<html><p>%s <i>(%s MB)</i></p></html>", bigData.getKey(), size));
        }
        form.setItems(problems.toArray(new String[problems.size()]));
        CustomButtonAction cancelAction = new CustomButtonAction("Quit", new Runnable() {
            @Override
            public void run() {

            }
        });
        CustomButtonAction okAction = new CustomButtonAction("Ignore and Continue", new Runnable() {
            @Override
            public void run() {

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

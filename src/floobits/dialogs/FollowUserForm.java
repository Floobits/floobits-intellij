package floobits.dialogs;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class FollowUserForm {
    private JPanel contentPanel;
    private JTable userTable;

    private class UserListModel extends AbstractTableModel {

        private HashMap<String, Boolean> data;
        private List<String> order = new ArrayList<String>();

        public void setData(HashMap<String, Boolean> data) {
            this.data = data;
            order.addAll(data.keySet());
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String username = order.get(rowIndex);
            return new FollowUserData(username, data.get(username));
        }
    }

    public class FollowUserData {
        public String username;
        public Boolean following;
        public FollowUserData(String username, Boolean following) {
            this.username = username;
            this.following = following;
        }
    }

    private class CheckboxRenderer extends JCheckBox implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            FollowUserData data = (FollowUserData) value;
            this.setText(data.username);
            this.setSelected(data.following);
            return this;
        }
    }

    public void setUsers(HashMap<String, Boolean> usersToChoose) {
        UserListModel tableModel = new UserListModel();
        userTable.setDefaultRenderer(FollowUserData.class, new CheckboxRenderer());
        tableModel.setData(usersToChoose);
        userTable.setModel(tableModel);
    }


    public JComponent getContentPanel() {
        return contentPanel;
    }
}

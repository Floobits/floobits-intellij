package floobits.dialogs;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
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

        public String getColumnName(int col) {
            if (col == 1) {
                return "User with edit permissions";
            }
            return "Follow changes";
        }


        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String username = order.get(rowIndex);
            if (columnIndex == 0) {
                return data.get(username);
            }
            return username;
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            String username = order.get(row);
            data.put(username, (Boolean) value);
            fireTableCellUpdated(row, col);
        }
    }

    public void setUsers(HashMap<String, Boolean> usersToChoose) {
        UserListModel tableModel = new UserListModel();
        tableModel.setData(usersToChoose);
        userTable.setModel(tableModel);
    }


    public JComponent getContentPanel() {
        return contentPanel;
    }
}

package floobits.dialogs;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FollowUserForm {
    private JPanel contentPanel;
    private JTable userTable;
    private UserListModel tableModel;

    public class UserListModel extends AbstractTableModel {

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
                return "Users with edit permissions";
            }
            return "Following changes";
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

        public List<String> toList() {
            List<String> userList = new ArrayList<String>();
            for (Map.Entry<String, Boolean> entry : data.entrySet()) {
                if (entry.getValue()) {
                    userList.add(entry.getKey());
                }
            }
            return userList;
        }
    }

    public void setUsers(HashMap<String, Boolean> usersToChoose) {
        tableModel = new UserListModel();
        tableModel.setData(usersToChoose);
        userTable.setModel(tableModel);
    }

    public UserListModel getUserModel () {
        return tableModel;
    }


    public JComponent getContentPanel() {
        return contentPanel;
    }
}

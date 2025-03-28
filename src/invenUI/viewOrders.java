package invenUI;

import db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class viewOrders {

    private JFrame frame;
    private JTable table;
    private int userId;
    private DefaultTableModel model;

    public viewOrders(int userId) {
        this.userId = userId;

        frame = new JFrame("Your Orders");
        frame.setSize(800, 400);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        model = new DefaultTableModel();
        model.addColumn("Order ID");
        model.addColumn("Product Name");
        model.addColumn("Quantity");
        model.addColumn("Total Price");
        model.addColumn("Status");
        model.addColumn("Action");  // Column for buttons

        table = new JTable(model);
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));

        loadOrders();

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void loadOrders() {
        model.setRowCount(0); // Clear table before loading new data

        try (Connection con = DBConnection.getConnection()) {

            String query = "SELECT o.order_id, p.name AS product_name, o.quantity, o.total_price, o.status " +
                    "FROM Orders o " +
                    "JOIN Products p ON o.product_id = p.product_id " +
                    "WHERE o.user_id = ? " +
                    "ORDER BY o.order_date DESC";

            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, userId);  // ✅ Correct usage of parameter
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                String name = rs.getString("product_name");
                int quantity = rs.getInt("quantity");
                double totalPrice = rs.getDouble("total_price");
                String status = rs.getString("status");

                Vector<Object> row = new Vector<>();
                row.add(orderId);
                row.add(name);
                row.add(quantity);
                row.add(totalPrice);
                row.add(status);
                row.add(status.equals("Processing") ? "Received" : ""); // Button label
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void markOrderAsReceived(int orderId) {
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/invenmgmt", "root", "Athithya@2004$$")) {
            String query = "UPDATE Orders SET status = 'Received' WHERE order_id = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, orderId);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(frame, "Order marked as received!");
            loadOrders(); // Refresh table after update
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null && value.equals("Received")) {
                setText("Received");
            } else {
                setText("");
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int selectedOrderId;
        private boolean isClicked;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);

            button.addActionListener(e -> {
                if (isClicked) {
                    markOrderAsReceived(selectedOrderId);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            selectedOrderId = (int) table.getValueAt(row, 0);
            button.setText(value != null ? value.toString() : "");
            isClicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            isClicked = false;
            return "Received";
        }

        @Override
        public boolean stopCellEditing() {
            isClicked = false;
            return super.stopCellEditing();
        }
    }
}

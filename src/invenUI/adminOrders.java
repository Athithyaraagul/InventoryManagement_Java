package invenUI;

import db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class adminOrders {
    private final DefaultTableModel model;

    public adminOrders() {
        JFrame frame = new JFrame("Orders Placed");
        frame.setSize(900, 500);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        model = new DefaultTableModel();
        model.addColumn("Order ID");
        model.addColumn("Username");
        model.addColumn("Total Items");
        model.addColumn("Total Price");
        model.addColumn("Status");

        JTable table = new JTable(model);
        loadOrders();

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void loadOrders() {
        model.setRowCount(0);

        try {
            Connection connection = DBConnection.getConnection();
            String query = "SELECT o.order_id, u.username, COUNT(o.product_id) AS total_items, " +
                    "o.total_price, o.status " +
                    "FROM Orders o " +
                    "JOIN Users u ON o.user_id = u.user_id " +
                    "GROUP BY o.order_id, u.username, o.total_price, o.status";
            assert connection != null;
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                String username = rs.getString("username");
                int totalItems = rs.getInt("total_items");
                double totalPrice = rs.getDouble("total_price");
                String status = rs.getString("status");

                model.addRow(new Object[]{orderId, username, totalItems, totalPrice, status});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

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
        frame.setSize(1200, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        model = new DefaultTableModel();
        model.addColumn("Order ID");
        model.addColumn("Username");
        model.addColumn("User Address");
        model.addColumn("Product ID");
        model.addColumn("Product Name");
        model.addColumn("Quantity");
        model.addColumn("Price (Individual)");
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
            String query = "SELECT o.order_id, u.username, u.userAddress, p.product_id, p.name AS product_name, " +
                    "o.quantity, p.price, (p.price * o.quantity) AS total_price, o.status " +
                    "FROM Orders o " +
                    "JOIN Users u ON o.user_id = u.user_id " +
                    "JOIN Products p ON o.product_id = p.product_id";
            assert connection != null;
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                String username = rs.getString("username");
                String address = rs.getString("userAddress");
                int productId = rs.getInt("product_id");
                String productName = rs.getString("product_name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double totalPrice = rs.getDouble("total_price");
                String status = rs.getString("status");

                model.addRow(new Object[]{orderId, username, address, productId, productName, quantity, price, totalPrice, status});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
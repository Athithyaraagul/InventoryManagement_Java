package invenUI;

import db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;

public class userUI {
    private JFrame frame;
    private JLabel titleLabel;
    private JTable table;
    private JButton orderButton, viewOrdersButton, logoutButton;
    private int userId;
    private String username;

    public userUI(int userId) {
        this.userId = userId;
        this.username = fetchUsername(userId);

        frame = new JFrame("User Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        titleLabel = new JLabel("Welcome Back, " + username, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        frame.add(titleLabel, BorderLayout.NORTH);

        table = new JTable();
        customizeTable();
        loadAvailableProducts();

        orderButton = new JButton("Order");
        viewOrdersButton = new JButton("View Orders");
        logoutButton = new JButton("Logout");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(orderButton);
        buttonPanel.add(viewOrdersButton);
        buttonPanel.add(logoutButton);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        orderButton.addActionListener(e -> openOrderWindow());
        viewOrdersButton.addActionListener(e -> openViewOrdersWindow());
        logoutButton.addActionListener(e -> logout());

        frame.setVisible(true);
    }

    private String fetchUsername(int userId) {
        try (Connection con = getConnection();
             PreparedStatement pst = con.prepareStatement("SELECT username FROM Users WHERE user_id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getString("username") : "User";
        } catch (SQLException e) {
            e.printStackTrace();
            return "User";
        }
    }

    private void customizeTable() {
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(25);
    }

    private void loadAvailableProducts() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT product_id, name, description, price, stock FROM Products WHERE stock > 0")) {
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("Product ID");
            model.addColumn("Name");
            model.addColumn("Description");
            model.addColumn("Price");
            model.addColumn("Stock");

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                });
            }
            table.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadProductsForDropdown(JComboBox<String> dropdown) {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT product_id, name FROM Products WHERE stock > 0")) {
            while (rs.next()) {
                dropdown.addItem(rs.getInt("product_id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openOrderWindow() {
        JDialog orderDialog = new JDialog(frame, "Place Order", true);
        orderDialog.setSize(500, 300);
        orderDialog.setLayout(new GridLayout(3, 2, 10, 10));

        JLabel productLabel = new JLabel("Select Product:");
        JComboBox<String> productDropdown = new JComboBox<>();
        loadProductsForDropdown(productDropdown);
        JLabel quantityLabel = new JLabel("Quantity:");
        JTextField quantityField = new JTextField(10);
        JButton placeOrderButton = new JButton("Place Order");
        JButton cancelButton = new JButton("Cancel");

        placeOrderButton.addActionListener(e -> {
            try {
                String selectedProduct = (String) productDropdown.getSelectedItem();
                if (selectedProduct == null || selectedProduct.isEmpty()) {
                    JOptionPane.showMessageDialog(orderDialog, "Please select a product.");
                    return;
                }
                int productId = Integer.parseInt(selectedProduct.split(" - ")[0]);
                int quantity = Integer.parseInt(quantityField.getText());
                placeOrder(productId, quantity);
                orderDialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(orderDialog, "Invalid input. Please enter a valid number.");
            }
        });

        cancelButton.addActionListener(e -> orderDialog.dispose());

        orderDialog.add(productLabel);
        orderDialog.add(productDropdown);
        orderDialog.add(quantityLabel);
        orderDialog.add(quantityField);
        orderDialog.add(placeOrderButton);
        orderDialog.add(cancelButton);
        orderDialog.setLocationRelativeTo(frame);
        orderDialog.setVisible(true);
    }

    private void placeOrder(int productId, int quantity) {
        try (Connection con = getConnection()) {

            // Fetch username from Users table
            String getUserQuery = "SELECT username FROM Users WHERE user_id = ?";
            PreparedStatement userStmt = con.prepareStatement(getUserQuery);
            userStmt.setInt(1, userId);
            ResultSet userRs = userStmt.executeQuery();
            String username = "Guest";  // Default value
            if (userRs.next()) {
                username = userRs.getString("username");
            }

            // Fetch product name
            String getProductQuery = "SELECT name, price FROM Products WHERE product_id = ?";
            PreparedStatement productStmt = con.prepareStatement(getProductQuery);
            productStmt.setInt(1, productId);
            ResultSet productRs = productStmt.executeQuery();

            String productName = "";
            double price = 0.0;

            if (productRs.next()) {
                productName = productRs.getString("name");
                price = productRs.getDouble("price");  // Fetch the price from the database
            }

            // Calculate total price
            double totalPrice = quantity * price;

            // Insert order into Orders table
            String orderQuery = "INSERT INTO Orders(user_id, username, product_id, product_name, quantity, price, total_price, status, order_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

            PreparedStatement orderStmt = con.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);

            orderStmt.setInt(1, userId);          // user_id
            orderStmt.setString(2, username);     // username
            orderStmt.setInt(3, productId);       // product_id
            orderStmt.setString(4, productName);  // product_name
            orderStmt.setInt(5, quantity);        // quantity
            orderStmt.setDouble(6, price);        // price
            orderStmt.setDouble(7, totalPrice);   // total_price
            orderStmt.setString(8, "Processing"); // status

            // Update product stock
            String query = "UPDATE Products SET stock = stock - ? WHERE product_id = ? AND stock >= ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, quantity);
            pst.setInt(2, productId);
            pst.setInt(3, quantity);

            orderStmt.executeUpdate();
            int affectedRows = pst.executeUpdate();
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(frame, "Order Placed Successfully!");
                loadAvailableProducts();
            } else {
                JOptionPane.showMessageDialog(frame, "Insufficient stock available!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    private void openViewOrdersWindow() {
        new viewOrders(userId);
    }

    private void logout() {
        frame.dispose();
        new landingPage();
    }

    public static void main(String[] args) {
        new userUI(1);
    }
}

//package invenUI;
//
//import javax.swing.*;
//import javax.swing.table.DefaultTableModel;
//import javax.swing.table.JTableHeader;
//import java.awt.*;
//import java.sql.*;
//
//public class userUI {
//
//    private JFrame frame;
//    private JLabel titleLabel;
//    private JTable table;
//    private JButton orderButton, viewOrdersButton, logoutButton;
//    private int userId;
//    private String username;
//
//    public userUI(int userId) {
//        this.userId = userId;
//        this.username = fetchUsername(userId);
//
//        frame = new JFrame("User Dashboard");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(1000, 600);
//        frame.setLocationRelativeTo(null);
//        frame.setLayout(new BorderLayout());
//
//        titleLabel = new JLabel("Welcome Back, " + username, SwingConstants.CENTER);
//        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
//        frame.add(titleLabel, BorderLayout.NORTH);
//
//        table = new JTable();
//        customizeTable();
//        loadAvailableProducts();
//
//        orderButton = new JButton("Order");
//        viewOrdersButton = new JButton("View Orders");
//        logoutButton = new JButton("Logout");
//
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.add(orderButton);
//        buttonPanel.add(viewOrdersButton);
//        buttonPanel.add(logoutButton);
//
//        frame.add(new JScrollPane(table), BorderLayout.CENTER);
//        frame.add(buttonPanel, BorderLayout.SOUTH);
//
//        orderButton.addActionListener(e -> openOrderWindow());
//        viewOrdersButton.addActionListener(e -> openViewOrdersWindow());
//        logoutButton.addActionListener(e -> logout());
//
//        frame.setVisible(true);
//    }
//
//    private String fetchUsername(int userId) {
//        String username = "User";  // Default
//        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/invenmgmt", "root", "Athithya@2004$$")) {
//            String query = "SELECT username FROM Users WHERE user_id = ?";
//            PreparedStatement pst = con.prepareStatement(query);
//            pst.setInt(1, userId);
//            ResultSet rs = pst.executeQuery();
//            if (rs.next()) {
//                username = rs.getString("username");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return username;
//    }
//
//    private void customizeTable() {
//        JTableHeader header = table.getTableHeader();
//        header.setFont(new Font("SansSerif", Font.BOLD, 14));
//        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
//        table.setRowHeight(25);
//    }
//
//    private void loadAvailableProducts() {
//        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/invenmgmt", "root", "Athithya@2004$$")) {
//            String query = "SELECT product_id, name, description, price, stock FROM Products WHERE stock > 0";
//            PreparedStatement pst = con.prepareStatement(query);
//            ResultSet rs = pst.executeQuery();
//
//            DefaultTableModel model = new DefaultTableModel();
//            model.addColumn("Product ID");
//            model.addColumn("Name");
//            model.addColumn("Description");
//            model.addColumn("Price");
//            model.addColumn("Stock");
//
//            while (rs.next()) {
//                model.addRow(new Object[]{
//                        rs.getInt("product_id"),
//                        rs.getString("name"),
//                        rs.getString("description"),
//                        rs.getDouble("price"),
//                        rs.getInt("stock")
//                });
//            }
//
//            table.setModel(model);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void openOrderWindow() {
//        JDialog orderDialog = new JDialog(frame, "Place Order", true);
//        orderDialog.setSize(500, 300);
//        orderDialog.setLayout(new GridLayout(3, 2, 10, 10));
//
//        JLabel productLabel = new JLabel("Select Product:");
//        JComboBox<String> productDropdown = new JComboBox<>();
//        loadProductsForDropdown(productDropdown);
//        JLabel quantityLabel = new JLabel("Quantity:");
//        JTextField quantityField = new JTextField(10);
//        JButton placeOrderButton = new JButton("Place Order");
//        JButton cancelButton = new JButton("Cancel");
//
//        placeOrderButton.addActionListener(e -> {
//            String selectedProduct = (String) productDropdown.getSelectedItem();
//            if (selectedProduct == null || selectedProduct.isEmpty()) {
//                JOptionPane.showMessageDialog(orderDialog, "Please select a product.");
//                return;
//            }
//            int productId = Integer.parseInt(selectedProduct.split(" - ")[0]);
//            int quantity = Integer.parseInt(quantityField.getText());
//            double price = getProductPrice(productId);
//
//            placeOrder(productId, quantity, price);
//            orderDialog.dispose();
//        });
//
//        cancelButton.addActionListener(e -> orderDialog.dispose());
//
//        orderDialog.add(productLabel);
//        orderDialog.add(productDropdown);
//        orderDialog.add(quantityLabel);
//        orderDialog.add(quantityField);
//        orderDialog.add(placeOrderButton);
//        orderDialog.add(cancelButton);
//
//        orderDialog.setLocationRelativeTo(frame);
//        orderDialog.setVisible(true);
//    }
//
//    private void loadProductsForDropdown(JComboBox<String> dropdown) {
//        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/invenmgmt", "root", "Athithya@2004$$")) {
//            String query = "SELECT product_id, name FROM Products WHERE stock > 0";
//            PreparedStatement pst = con.prepareStatement(query);
//            ResultSet rs = pst.executeQuery();
//
//            while (rs.next()) {
//                dropdown.addItem(rs.getInt("product_id") + " - " + rs.getString("name"));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private double getProductPrice(int productId) {
//        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/invenmgmt", "root", "Athithya@2004$$")) {
//            String query = "SELECT price FROM Products WHERE product_id = ?";
//            PreparedStatement pst = con.prepareStatement(query);
//            pst.setInt(1, productId);
//            ResultSet rs = pst.executeQuery();
//            if (rs.next()) {
//                return rs.getDouble("price");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return 0;
//    }
//
//    private void placeOrder(int productId, int quantity, double price) {
//        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/invenmgmt", "root", "Athithya@2004$$")) {
//
//            // Fetch username from Users table
//            String getUserQuery = "SELECT username FROM Users WHERE user_id = ?";
//            PreparedStatement userStmt = con.prepareStatement(getUserQuery);
//            userStmt.setInt(1, userId);
//            ResultSet userRs = userStmt.executeQuery();
//            String username = "Guest";  // Default value
//            if (userRs.next()) {
//                username = userRs.getString("username");
//            }
//
//            // Fetch product name
//            String getProductQuery = "SELECT name FROM Products WHERE product_id = ?";
//            PreparedStatement productStmt = con.prepareStatement(getProductQuery);
//            productStmt.setInt(1, productId);
//            ResultSet productRs = productStmt.executeQuery();
//            String productName = "";
//            if (productRs.next()) {
//                productName = productRs.getString("name");
//            }
//
//            // Calculate total price
//            double totalPrice = quantity * price;
//
//            // Insert order into Orders table
//            String orderQuery = "INSERT INTO Orders(user_id, username, product_id, product_name, quantity, price, total_price, status, order_date) " +
//                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
//
//            PreparedStatement orderStmt = con.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
//
//            orderStmt.setInt(1, userId);          // user_id
//            orderStmt.setString(2, username);     // username
//            orderStmt.setInt(3, productId);       // product_id
//            orderStmt.setString(4, productName);  // product_name
//            orderStmt.setInt(5, quantity);        // quantity (MISSING IN PREVIOUS VERSION)
//            orderStmt.setDouble(6, price);        // price
//            orderStmt.setDouble(7, totalPrice);   // total_price
//            orderStmt.setString(8, "Processing");// status
//
//            orderStmt.executeUpdate();
//
//            JOptionPane.showMessageDialog(frame, "Order Placed Successfully!");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private double getProductPrice(Connection con, int productId) throws SQLException {
//        String query = "SELECT price FROM Products WHERE product_id = ?";
//        PreparedStatement pst = con.prepareStatement(query);
//        pst.setInt(1, productId);
//        ResultSet rs = pst.executeQuery();
//        if (rs.next()) {
//            return rs.getDouble("price");
//        }
//        return 0.0;
//    }
//
//
//    private void openViewOrdersWindow() {
//        new viewOrders(userId);
//    }
//
//    private void logout() {
//        frame.dispose();
//        new landingPage();
//    }
//
//    public static void main(String[] args) {
//        new userUI(1);
//    }
//}
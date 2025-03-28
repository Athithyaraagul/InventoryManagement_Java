package invenUI;

import db.DBConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;

public class adminUI {
    private JTable table;
    private DefaultTableModel tableModel;
    private Connection connection;

    adminUI() throws SQLException {
        connection = DBConnection.getConnection();

        // UI
        JFrame frame = new JFrame("Inventory Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLayout(null);

        JLabel titleLabel = new JLabel("Product Description", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(250, 10, 1300, 30);
        frame.add(titleLabel);

        // Table
        String[] columns = {"Product ID", "Name", "Description", "Price", "Stock Quantity"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);

        table.setFont(new Font("Arial", Font.PLAIN, 15));
        table.setRowHeight(25);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 16));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(150, 50, 1500, 750);
        frame.add(scrollPane);

        // Buttons
        JButton insertButton = new JButton("Insert");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton orderStatusButton = new JButton("Order Status");
        JButton logoutButton = new JButton("Logout");

        insertButton.setBounds(20, 50, 105, 30);
        updateButton.setBounds(20, 100, 105, 30);
        deleteButton.setBounds(20, 150, 105, 30);
        orderStatusButton.setBounds(20, 250, 105, 30);
        logoutButton.setBounds(20, 200, 100, 30);

        frame.add(insertButton);
        frame.add(updateButton);
        frame.add(deleteButton);
        frame.add(orderStatusButton);
        frame.add(logoutButton);

        loadProducts();

        insertButton.addActionListener(e -> insertProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        orderStatusButton.addActionListener(e -> new adminOrders());
        logoutButton.addActionListener(e -> logout(frame));

        frame.setVisible(true);
    }

    private void loadProducts() {
        try {
            tableModel.setRowCount(0); // Clear existing rows
            String query = "SELECT * FROM Products";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("product_id");
                String name = rs.getString("name");
                String desc = rs.getString("description");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                tableModel.addRow(new Object[]{id, name, desc, price, stock});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error Loading Products");
        }
    }

    private void insertProduct() {
        JTextField nameField = new JTextField();
        JTextField descField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField stockField = new JTextField();

        Object[] fields = {
                "Name:", nameField,
                "Description:", descField,
                "Price:", priceField,
                "Stock Quantity:", stockField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Insert Product", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String query = "INSERT INTO Products (name, description, price, stock) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, descField.getText());
                stmt.setDouble(3, Double.parseDouble(priceField.getText()));
                stmt.setInt(4, Integer.parseInt(stockField.getText()));

                stmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "Product Added Successfully!");
                loadProducts();
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error Adding Product");
            }
        }
    }

    private void updateProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Select a product to update!");
            return;
        }

        int productId = (int) tableModel.getValueAt(selectedRow, 0);
        JTextField nameField = new JTextField(tableModel.getValueAt(selectedRow, 1).toString());
        JTextField descField = new JTextField(tableModel.getValueAt(selectedRow, 2).toString());
        JTextField priceField = new JTextField(tableModel.getValueAt(selectedRow, 3).toString());
        JTextField stockField = new JTextField(tableModel.getValueAt(selectedRow, 4).toString());

        Object[] fields = {
                "Name:", nameField,
                "Description:", descField,
                "Price:", priceField,
                "Stock Quantity:", stockField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Update Product", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String query = "UPDATE Products SET name=?, description=?, price=?, stock=? WHERE product_id=?";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, descField.getText());
                stmt.setDouble(3, Double.parseDouble(priceField.getText()));
                stmt.setInt(4, Integer.parseInt(stockField.getText()));
                stmt.setInt(5, productId);

                stmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "Product Updated Successfully!");
                loadProducts();
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error Updating Product");
            }
        }
    }

    private void deleteProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Select a product to delete!");
            return;
        }

        int productId = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this product?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // First, delete related orders
                String deleteOrdersQuery = "DELETE FROM orders WHERE product_id = ?";
                PreparedStatement deleteOrdersStmt = connection.prepareStatement(deleteOrdersQuery);
                deleteOrdersStmt.setInt(1, productId);
                deleteOrdersStmt.executeUpdate();

                // Then, delete the product
                String deleteProductQuery = "DELETE FROM products WHERE product_id = ?";
                PreparedStatement deleteProductStmt = connection.prepareStatement(deleteProductQuery);
                deleteProductStmt.setInt(1, productId);
                deleteProductStmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Product deleted successfully!");
                loadProducts();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error Deleting Product");
            }
        }
    }


    private void logout(JFrame frame) {
        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to log out?", "Log Out", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                frame.dispose();
                new landingPage();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error Logging Out");
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        new adminUI();
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
//public class adminUI {
//    private JTable table;
//    private DefaultTableModel tableModel;
//    private Connection connection;
//
//    adminUI() {
//        connectToDatabase();
//
//        // UI
//        JFrame frame = new JFrame("Inventory Management System");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        frame.setLayout(null);
//
//        JLabel titleLabel = new JLabel("Product Description", SwingConstants.CENTER);
//        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
//        titleLabel.setBounds(250, 10, 1300, 30);
//        frame.add(titleLabel);
//
//        // Table
//        String[] columns = {"Product ID", "Name", "Description", "Price", "Stock Quantity"};
//        tableModel = new DefaultTableModel(columns, 0);
//        table = new JTable(tableModel);
//
//        table.setFont(new Font("Arial", Font.PLAIN, 15));
//        table.setRowHeight(25);
//
//
//        JTableHeader header = table.getTableHeader();
//        header.setFont(new Font("Arial", Font.BOLD, 16));
//        JScrollPane scrollPane = new JScrollPane(table);
//        scrollPane.setBounds(150, 50, 1500, 750);
//        frame.add(scrollPane);
//
//        // Buttons
//        JButton insertButton = new JButton("Insert");
//        JButton updateButton = new JButton("Update");
//        JButton deleteButton = new JButton("Delete");
//        JButton orderStatusButton = new JButton("Order Status");
//        JButton logoutButton = new JButton("Logout");
//
//        insertButton.setBounds(20, 50, 105, 30);
//        updateButton.setBounds(20, 100, 105, 30);
//        deleteButton.setBounds(20, 150, 105, 30);
//        orderStatusButton.setBounds(20, 250, 105, 30);
//        logoutButton.setBounds(20, 200, 100, 30);
//
//        frame.add(insertButton);
//        frame.add(updateButton);
//        frame.add(deleteButton);
//        frame.add(orderStatusButton);
//        frame.add(logoutButton);
//
//        loadProducts();
//
//        insertButton.addActionListener(e -> insertProduct());
//        updateButton.addActionListener(e -> updateProduct());
//        deleteButton.addActionListener(e -> deleteProduct());
//        orderStatusButton.addActionListener(e -> new adminOrders());
//        logoutButton.addActionListener(e -> logout(frame));
//
//        frame.setVisible(true);
//    }
//
//    private void connectToDatabase() {
//        try {
//            String url = "jdbc:mysql://localhost:3306/invenmgmt";
//            String user = "root";
//            String password = "Athithya@2004$$";
//
//            connection = DriverManager.getConnection(url, user, password);
//            System.out.println("Database Connected Successfully!");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void loadProducts() {
//        try {
//            tableModel.setRowCount(0); // Clear existing rows
//            String query = "SELECT * FROM Products";
//            PreparedStatement stmt = connection.prepareStatement(query);
//            ResultSet rs = stmt.executeQuery();
//
//            while (rs.next()) {
//                int id = rs.getInt("product_id");
//                String name = rs.getString("name");
//                String desc = rs.getString("description");
//                double price = rs.getDouble("price");
//                int stock = rs.getInt("stock");
//
//                tableModel.addRow(new Object[]{id, name, desc, price, stock});
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            JOptionPane.showMessageDialog(null, "Error Loading Products");
//        }
//    }
//
//    private void insertProduct() {
//        JTextField nameField = new JTextField();
//        JTextField descField = new JTextField();
//        JTextField priceField = new JTextField();
//        JTextField stockField = new JTextField();
//
//        Object[] fields = {
//                "Name:", nameField,
//                "Description:", descField,
//                "Price:", priceField,
//                "Stock Quantity:", stockField
//        };
//
//        int option = JOptionPane.showConfirmDialog(null, fields, "Insert Product", JOptionPane.OK_CANCEL_OPTION);
//        if (option == JOptionPane.OK_OPTION) {
//            try {
//                String query = "INSERT INTO Products (name, description, price, stock) VALUES (?, ?, ?, ?)";
//                PreparedStatement stmt = connection.prepareStatement(query);
//                stmt.setString(1, nameField.getText());
//                stmt.setString(2, descField.getText());
//                stmt.setDouble(3, Double.parseDouble(priceField.getText()));
//                stmt.setInt(4, Integer.parseInt(stockField.getText()));
//
//                stmt.executeUpdate();
//                JOptionPane.showMessageDialog(null, "Product Added Successfully!");
//                loadProducts();
//            } catch (SQLException | NumberFormatException e) {
//                e.printStackTrace();
//                JOptionPane.showMessageDialog(null, "Error Adding Product");
//            }
//        }
//    }
//
//    private void updateProduct() {
//        int selectedRow = table.getSelectedRow();
//        if (selectedRow == -1) {
//            JOptionPane.showMessageDialog(null, "Select a product to update!");
//            return;
//        }
//
//        int productId = (int) tableModel.getValueAt(selectedRow, 0);
//        JTextField nameField = new JTextField(tableModel.getValueAt(selectedRow, 1).toString());
//        JTextField descField = new JTextField(tableModel.getValueAt(selectedRow, 2).toString());
//        JTextField priceField = new JTextField(tableModel.getValueAt(selectedRow, 3).toString());
//        JTextField stockField = new JTextField(tableModel.getValueAt(selectedRow, 4).toString());
//
//        Object[] fields = {
//                "Name:", nameField,
//                "Description:", descField,
//                "Price:", priceField,
//                "Stock Quantity:", stockField
//        };
//
//        int option = JOptionPane.showConfirmDialog(null, fields, "Update Product", JOptionPane.OK_CANCEL_OPTION);
//        if (option == JOptionPane.OK_OPTION) {
//            try {
//                String query = "UPDATE Products SET name=?, description=?, price=?, stock=? WHERE product_id=?";
//                PreparedStatement stmt = connection.prepareStatement(query);
//                stmt.setString(1, nameField.getText());
//                stmt.setString(2, descField.getText());
//                stmt.setDouble(3, Double.parseDouble(priceField.getText()));
//                stmt.setInt(4, Integer.parseInt(stockField.getText()));
//                stmt.setInt(5, productId);
//
//                stmt.executeUpdate();
//                JOptionPane.showMessageDialog(null, "Product Updated Successfully!");
//                loadProducts();
//            } catch (SQLException | NumberFormatException e) {
//                e.printStackTrace();
//                JOptionPane.showMessageDialog(null, "Error Updating Product");
//            }
//        }
//    }
//
//    private void deleteProduct() {
//        int selectedRow = table.getSelectedRow();
//        if (selectedRow == -1) {
//            JOptionPane.showMessageDialog(null, "Select a product to delete!");
//            return;
//        }
//
//        int productId = (int) tableModel.getValueAt(selectedRow, 0);
//        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this product?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
//
//        if (confirm == JOptionPane.YES_OPTION) {
//            try {
//                String query = "DELETE FROM Products WHERE product_id=?";
//                PreparedStatement stmt = connection.prepareStatement(query);
//                stmt.setInt(1, productId);
//                stmt.executeUpdate();
//
//                JOptionPane.showMessageDialog(null, "Product Deleted Successfully!");
//                loadProducts();
//            } catch (SQLException e) {
//                e.printStackTrace();
//                JOptionPane.showMessageDialog(null, "Error Deleting Product");
//            }
//        }
//    }
//
//    private void logout(JFrame frame) {
//        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to log out?", "Log Out", JOptionPane.YES_NO_OPTION);
//        if (confirm == JOptionPane.YES_OPTION) {
//            try {
//                frame.dispose();
//                new landingPage();
//            } catch (Exception e) {
//                e.printStackTrace();
//                JOptionPane.showMessageDialog(null, "Error Logging Out");
//            }
//        }
//
//    }
//
//    public static void main(String[] args) {
//        new adminUI();
//    }
//}

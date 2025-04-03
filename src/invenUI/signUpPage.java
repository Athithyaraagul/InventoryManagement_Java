package invenUI;

import db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class signUpPage {
    signUpPage() {
        JFrame frame = new JFrame("Sign Up");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 450);
        frame.setLayout(null);

        JLabel titleLabel = new JLabel("Sign Up Page", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(150, 20, 200, 30);
        frame.add(titleLabel);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(100, 70, 100, 25);
        JTextField userText = new JTextField();
        userText.setBounds(200, 70, 200, 25);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setBounds(100, 110, 100, 25);
        JTextField emailText = new JTextField();
        emailText.setBounds(200, 110, 200, 25);

        JLabel addressLabel = new JLabel("Address:");
        addressLabel.setBounds(100, 150, 100, 25);
        JTextField addressText = new JTextField();
        addressText.setBounds(200, 150, 200, 25);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(100, 190, 100, 25);
        JPasswordField passText = new JPasswordField();
        passText.setBounds(200, 190, 200, 25);

        JButton registerButton = new JButton("Register");
        registerButton.setBounds(200, 230, 100, 30);

        JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setBounds(100, 270, 300, 25);
        messageLabel.setForeground(Color.RED);

        frame.add(userLabel);
        frame.add(userText);
        frame.add(emailLabel);
        frame.add(emailText);
        frame.add(addressLabel);
        frame.add(addressText);
        frame.add(passLabel);
        frame.add(passText);
        frame.add(registerButton);
        frame.add(messageLabel);

        registerButton.addActionListener(_ -> {
            String username = userText.getText();
            String email = emailText.getText();
            String userAddress = addressText.getText();
            String password = new String(passText.getPassword());

            if (username.isEmpty() || email.isEmpty() || userAddress.isEmpty() || password.isEmpty()) {
                messageLabel.setText("All fields are required!");
                return;
            }

            if (!email.contains("@")) {
                messageLabel.setText("Invalid email format!");
                return;
            }

            boolean success = registerUser(username, email, userAddress, password);
            if (success) {
                JOptionPane.showMessageDialog(frame, "User Registered Successfully!");
                frame.dispose();
                SwingUtilities.invokeLater(landingPage::new);
            } else {
                messageLabel.setText("Registration failed. Try again!");
            }
        });

        frame.setVisible(true);
    }

    private boolean registerUser(String username, String email, String userAddress, String password) {
        String insertQuery = "INSERT INTO Users (username, email, userAddress, password) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            assert conn != null;
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, userAddress);
                pstmt.setString(4, password);
                int rowsInserted = pstmt.executeUpdate();

                return rowsInserted > 0;

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

}

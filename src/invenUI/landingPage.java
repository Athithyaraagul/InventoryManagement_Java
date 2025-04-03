package invenUI;

import db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class landingPage {
    private final JFrame frame;
    private final JTextField userText;
    private final JTextField emailText;
    private final JPasswordField passText;
    private final JLabel messageLabel;

    public landingPage() {
        frame = new JFrame("Login");
        frame.setSize(500, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        frame.setLocationRelativeTo(null);

        JLabel titleLabel = new JLabel("Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBounds(200, 10, 100, 30);
        frame.add(titleLabel);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(100, 50, 100, 30);
        userText = new JTextField();
        userText.setBounds(200, 50, 200, 30);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setBounds(100, 90, 100, 30);
        emailText = new JTextField();
        emailText.setBounds(200, 90, 200, 30);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(100, 130, 100, 30);
        passText = new JPasswordField();
        passText.setBounds(200, 130, 200, 30);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(200, 180, 90, 30);

        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setBounds(310, 180, 90, 30);

        messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setBounds(100, 220, 300, 30);
        messageLabel.setForeground(Color.RED);

        frame.add(userLabel);
        frame.add(userText);
        frame.add(emailLabel);
        frame.add(emailText);
        frame.add(passLabel);
        frame.add(passText);
        frame.add(loginButton);
        frame.add(signUpButton);
        frame.add(messageLabel);

        loginButton.addActionListener(_ -> authenticateUser());

        signUpButton.addActionListener(_ -> {
            frame.dispose();
            new signUpPage();
        });

        frame.setVisible(true);
    }

    private void authenticateUser() {
        String username = userText.getText();
        String email = emailText.getText();
        String password = new String(passText.getPassword());

        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT user_id, role FROM Users WHERE (username = ? OR email = ?) AND password = ?";
            assert conn != null;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");

                frame.dispose();

                if ("admin".equals(role)) {
                    new adminUI();
                } else {
                    new userUI(userId);
                }
            } else {
                messageLabel.setText("Invalid credentials! Try again.");
            }

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            messageLabel.setText("Database error.");
        }
    }

    public static void main(String[] args) {
        new landingPage();
    }
}
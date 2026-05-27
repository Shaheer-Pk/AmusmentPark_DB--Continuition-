package com.amusementpark;

import com.amusementpark.model.AdminDAO;
import com.amusementpark.model.Admin;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private final AdminDAO adminDAO = new AdminDAO();

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Client-side validation ───────────────────────────────────────
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required.");
            return;
        }

        // ── Async DB auth — never block the FX thread ───────────────────
        loginButton.setDisable(true);
        loginButton.setText("Signing in…");
        errorLabel.setText("");

        Task<Admin> authTask = new Task<>() {
            @Override
            protected Admin call() throws SQLException {
                return adminDAO.authenticate(email, password);
            }
        };

        authTask.setOnSucceeded(e -> {
            Admin admin = authTask.getValue();
            if (admin == null) {
                showError("Invalid email or password.");
                resetButton();
            } else {
                SessionManager.getInstance().login(admin);
                loadMainShell();
            }
        });

        authTask.setOnFailed(e -> {
            Throwable cause = authTask.getException();
            if (cause instanceof SQLException) {
                showError("Database error: " + cause.getMessage());
            } else {
                showError("Unexpected error. Check your connection.");
            }
            resetButton();
        });

        new Thread(authTask, "auth-thread").start();
    }

    private void loadMainShell() {
        try {
            URL mainUrl = getClass().getResource("/fxml/Main.fxml");
            URL style   = getClass().getResource("/css/style.css");

            FXMLLoader loader = new FXMLLoader(mainUrl);
            loader.setLocation(mainUrl);
            Scene scene = new Scene(loader.load(), 1280, 800);

            if (style != null) {
                scene.getStylesheets().add(style.toExternalForm());
            } else {
                System.out.println("WARNING: style.css not found on classpath");
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Apex Park — Admin Console");
        } catch (IOException ex) {
            System.out.println("=== Load failed ===");
            System.out.println(ex.getMessage());
            Throwable cause = ex.getCause();
            while (cause != null) {
                System.out.println("Caused by: " + cause);
                for (StackTraceElement e : cause.getStackTrace()) {
                    System.out.println("  " + e.getLineNumber() + " " + e.getMethodName());
                }
                cause = cause.getCause();
            }
        } //catch (NullPointerException ex) {
//            System.out.println(ex.toString());
//        }
    }

    private void showError(String msg) {
        Platform.runLater(() -> errorLabel.setText(msg));
    }

    private void resetButton() {
        Platform.runLater(() -> {
            loginButton.setDisable(false);
            loginButton.setText("Sign In");
        });
    }
}

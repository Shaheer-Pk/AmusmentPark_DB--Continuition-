package com.amusementpark;

import com.amusementpark.model.UserDAO;
import com.amusementpark.model.UserAccount;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;

public class LoginController {

    // Common Elements
    @FXML private Label loginHeaderTitle;
    @FXML private Label loginHeaderSubtitle;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button primaryActionButton;
    @FXML private Label toggleModeLabel;
    @FXML private Hyperlink toggleModeLink;

    // Registration UI Container Elements
    @FXML private VBox signUpFieldsContainer;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private DatePicker dobPicker;

    // Administrative Validation Controls
    @FXML private VBox adminPrivilegeContainer;
    @FXML private CheckBox adminCheckBox;
    @FXML private VBox masterKeyContainer;
    @FXML private PasswordField masterKeyField;

    private final UserDAO userDAO = new UserDAO();
    private final BooleanProperty signUpModeProperty = new SimpleBooleanProperty(false);
    
    // Constant key required to provision system level admins
    private static final String MASTER_CLEARANCE_KEY = "APEX2026";

    @FXML
    public void initialize() {
        // 1. Bind registration containers visibility and height management together
        signUpFieldsContainer.visibleProperty().bind(signUpModeProperty);
        signUpFieldsContainer.managedProperty().bind(signUpFieldsContainer.visibleProperty());

        adminPrivilegeContainer.visibleProperty().bind(signUpModeProperty);
        adminPrivilegeContainer.managedProperty().bind(adminPrivilegeContainer.visibleProperty());

        // 2. Master key panel opens only if registration is active AND admin option is toggled
        masterKeyContainer.visibleProperty().bind(adminCheckBox.selectedProperty().and(signUpModeProperty));
        masterKeyContainer.managedProperty().bind(masterKeyContainer.visibleProperty());
    }

    @FXML
    private void handleToggleMode() {
        // Switch the boolean status flag
        signUpModeProperty.set(!signUpModeProperty.get());
        errorLabel.setText("");
        
        if (signUpModeProperty.get()) {
            loginHeaderTitle.setText("Create Account");
            loginHeaderSubtitle.setText("Join Apex Park Management Systems");
            primaryActionButton.setText("Sign Up");
            toggleModeLabel.setText("Already have an account?");
            toggleModeLink.setText("Sign In");
        } else {
            loginHeaderTitle.setText("Sign In");
            loginHeaderSubtitle.setText("Access your Apex Park workspace");
            primaryActionButton.setText("Sign In");
            toggleModeLabel.setText("Don't have an account?");
            toggleModeLink.setText("Sign Up");
        }
    }

    @FXML
    private void handlePrimaryAction() {
        errorLabel.setText("");
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (signUpModeProperty.get()) {
            executeRegistrationFlow(email, password);
        } else {
            executeAuthenticationFlow(email, password);
        }
    }

    private void executeAuthenticationFlow(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email and password fields are required.");
            return;
        }

        primaryActionButton.setDisable(true);
        primaryActionButton.setText("Verifying credentials...");

        Task<UserAccount> loginTask = new Task<>() {
            @Override
            protected UserAccount call() throws SQLException {
                return userDAO.authenticate(email, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            UserAccount account = loginTask.getValue();
            if (account != null) {
                SessionManager.getInstance().login(account);
                navigateToDashboard(account.isIsAdmin());
            } else {
                errorLabel.setText("Invalid email address or security password.");
                resetActionButton("Sign In");
            }
        });

        loginTask.setOnFailed(e -> {
            errorLabel.setText("Database access timeout. Check connection pools.");
            resetActionButton("Sign In");
        });

        new Thread(loginTask).start();
    }

    private void executeRegistrationFlow(String email, String password) {
        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        LocalDate dob = dobPicker.getValue();

        // Structural UI Validation
        if (fName.isEmpty() || lName.isEmpty() || dob == null || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("All profile fields are mandatory for setup.");
            return;
        }

        if (password.length() < 6) {
            errorLabel.setText("Password security constraint unmet. Minimum 6 characters required.");
            return;
        }

        // Administrative Layer Guard Block
        boolean registerAsAdmin = adminCheckBox.isSelected();
        if (registerAsAdmin) {
            String validationKey = masterKeyField.getText();
            if (!MASTER_CLEARANCE_KEY.equals(validationKey)) {
                errorLabel.setText("Security Rejection: Administrative Master Clearance Key is incorrect.");
                return;
            }
        }

        primaryActionButton.setDisable(true);
        primaryActionButton.setText("Provisioning account...");

        Task<Boolean> registerTask = new Task<>() {
            @Override
            protected Boolean call() throws SQLException {
                return userDAO.registerUser(fName, lName, dob, email, password, registerAsAdmin);
            }
        };

        registerTask.setOnSucceeded(e -> {
            AlertHelper.showInfo("Registration Successful", "Account created securely. You may now sign in.");
            handleToggleMode(); // Automatically drop them back onto clean login layout screen
            resetActionButton("Sign In");
        });

        registerTask.setOnFailed(e -> {
            Throwable err = registerTask.getException();
            if (err != null && err.getMessage().contains("Duplicate entry")) {
                errorLabel.setText("An account with that email address already exists.");
            } else {
                errorLabel.setText("Transaction Processing Error: Operation aborted cleanly.");
            }
            resetActionButton("Sign Up");
        });

        new Thread(registerTask).start();
    }

    private void navigateToDashboard(boolean isAdmin) {
        Platform.runLater(() -> {
            try {
                // Dual path selection route diverter
                String targetFxml = isAdmin ? "/fxml/AdminMain.fxml" : "/fxml/CustomerMain.fxml";
                String windowTitle = isAdmin ? "Apex Park — Admin Management Console" : "Apex Park — Customer Digital Portal";

                URL fxmlUrl = getClass().getResource(targetFxml);
                URL cssUrl = getClass().getResource("/css/style.css");

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Scene scene = new Scene(loader.load(), 1280, 800);
                if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

                Stage stage = (Stage) primaryActionButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle(windowTitle);
                stage.centerOnScreen();
            } catch (IOException ex) {
                errorLabel.setText("System Routing Failure: Unable to paint target container panel context.");
                ex.printStackTrace();
            }
        });
    }

    private void resetActionButton(String originalText) {
        primaryActionButton.setDisable(false);
        primaryActionButton.setText(originalText);
    }
}
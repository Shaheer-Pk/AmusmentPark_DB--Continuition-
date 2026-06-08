package com.amusementpark.controller;

import com.amusementpark.dao.UserDAO;
import com.amusementpark.navigation.NavigationService;
import com.amusementpark.navigation.ViewType;
import com.amusementpark.util.AlertHelper;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// ─────────────────────────────────────────────────────────────────────────────
// SignupController — handles new account registration for Customers and Vendors.
//
// This controller is paired with signup.fxml.
// It is NEVER instantiated directly — JavaFX creates it when signup.fxml loads.
//
// FLOW:
//   1. AuthController navigates to signup.fxml via navigateToWithController()
//      and immediately calls initRole("Customer") or initRole("Vendor").
//   2. initRole() stores the role and updates the header label so the user
//      knows which account type they are creating.
//   3. User fills the form and clicks Submit.
//   4. handleSubmit() validates all fields, then calls UserDAO.createUser()
//      which runs a single DB transaction: User + Login + UserRole inserts.
//   5. On success: show confirmation, navigate back to login.
//   6. On failure: show specific error, stay on signup screen.
//
// WHAT THIS CONTROLLER DOES NOT DO:
//   - No password hashing (UserDAO + BCrypt handles that)
//   - No direct SQL
//   - No session initialisation (user must log in after registering)
//
// THREADING:
//   handleSubmit() fires one background Task named signupTask for userDAO queries.
//   onSucceeded pushes results back successful account creation to FX thread for label updates.
//   onFailed pushes account creation failure message to FX thread for label updates.
// ─────────────────────────────────────────────────────────────────────────────
public class SignupController {

    // ── FXML Field Injection ──────────────────────────────────────────────────

    @FXML private Label         roleLabel;       // Shows "Customer Account" or "Vendor Account"
    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     phoneField;
    @FXML private TextField     dobField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button submitBtn;       
    @FXML private Button goBackBtn;

    // ── State ─────────────────────────────────────────────────────────────────

    // Stored by initRole() — passed to UserDAO.createUser() on submit.
    // Either "Customer" or "Vendor". Never null when handleSubmit() runs
    // because AuthController always calls initRole() before the user can click.
    private String role;

    private final UserDAO userDAO = new UserDAO();

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Called by AuthController immediately after signup.fxml loads.
     * Sets the role this signup form is creating and updates the header label.
     *
     * This runs BEFORE the user sees the screen — by the time they read
     * "Vendor Account" the role is already stored and ready for submission.
     *
     * @param role either "Customer" or "Vendor"
     */
    public void initRole(String role) {
        this.role = role;
        // Update the label so the user knows which account type they're creating.
        roleLabel.setText(role + " Account Registration");
    }

    // ── Submit Handler ────────────────────────────────────────────────────────

    /**
     * Called when the Submit button is clicked (fx:onAction in signup.fxml).
     *
     * Validates every field, then delegates to UserDAO for the DB transaction.
     * Stays on the signup screen if anything is wrong — the user fixes and retries.
     * Navigates to login only after a confirmed successful insert.
     */
    @FXML
    private void handleSubmit() {

        // ── Collect field values ──────────────────────────────────────────────
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String phone     = phoneField.getText().trim();
        String dobText    = dobField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();

        // ── Validate: no empty fields (dob is handled seperately) ─────────────────────────────────────────
        if (firstName.isEmpty() || lastName.isEmpty() || dobText.isEmpty()
                || phone.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            AlertHelper.showError("Missing Fields", "Please fill in all fields.");
            return;
        }

        // ── The Absolute Control Wringer: Parse raw text to LocalDate ──────────
        LocalDate dob;
        try {
            // Enforce strict ISO-8601 layout (yyyy-MM-dd)
            DateTimeFormatter strictFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            dob = LocalDate.parse(dobText, strictFormatter);
        } catch (DateTimeParseException e) {
            // If they used slashes, wrong ordering, or typed gibberish, it catches here!
            AlertHelper.showError(
                "Invalid Date Format", 
                "Date of birth must use the format YYYY-MM-DD.\nExample: 2007-04-09"
            );
            return;
        }

        // ── Validate: passwords match ─────────────────────────────────────────
        if (!password.equals(confirm)) {
            AlertHelper.showError("Password Mismatch", "Passwords do not match.");
            confirmPasswordField.clear();
            return;
        }

        // ── Validate: minimum password length ─────────────────────────────────
        // BCrypt will hash anything, but we enforce a floor on the plain-text
        // side so users don't register with a single-character password.
        if (password.length() < 8) {
            AlertHelper.showError("Weak Password", "Password must be at least 8 characters.");
            return;
        }

        // ── Validate: date of birth is in the past ────────────────────────────
        if (!dob.isBefore(LocalDate.now())) {
            AlertHelper.showError("Invalid Date", "Date of birth must be in the past.");
            return;
        }

        
        /**
         * This is the point mentioned in UserDAO.java which can create the ghost id
         * Due to e.getErrorCode() == 1062
         * We offload this task to a worker thread using Task
         */
        // ── Attempt DB insert ─────────────────────────────────────────────────

        // Disable the buttons to prevent double-clicking potentially interrupting the task below
        if (submitBtn != null) submitBtn.setDisable(true);
        if (goBackBtn != null) goBackBtn.setDisable(true);

        Task<Void> signupTask = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                //Safe backend database transaction offloaded to a worker thread
                userDAO.createUser(firstName, lastName, phone, dob, email, password, role);
                return null;
            }
        };

        /*
         * Based on how task is executed now we communicate 
         * it back to the single UI JavaFX thread  
         */
        signupTask.setOnSucceeded(event -> {
            // Success — tell the user, then go back to login.
            AlertHelper.showInfo("Account Created",
                "Your " + role + " account has been created. Please log in.");
            NavigationService.navigateTo(ViewType.LOGIN);
        });

        signupTask.setOnFailed(event -> {
            Throwable exception = signupTask.getException();

            

            if (exception instanceof SQLException e) {
                // MySQL error code 1062 = duplicate entry on a UNIQUE column.
                // The Login table has UNIQUE on Email — this is the "already registered" case.
                if (e.getErrorCode() == 1062) {
                    AlertHelper.showError("Email Taken",
                        "An account with this email already exists.");
                } else {
                    // Unexpected DB error — show generic message to user,
                    // print full trace to console for the developer.
                    AlertHelper.showError("Registration Failed",
                        "Something went wrong. Please try again.");
                    e.printStackTrace();
                }
            }
            else {
                // CATCH-ALL FOR UNEXPECTED FAILS: (NPEs, ClassNotFound, OutOfMemory, etc.)
                AlertHelper.showError("System Error", 
                    "An unexpected application error occurred: " + exception.getMessage());
                exception.printStackTrace();
            }

            // Reset the buttons back so user can fix his errors and retry again
            submitBtn.setDisable(false);
            goBackBtn.setDisable(false); 
        });

        // ── Run the Task on a daemon thread ────────────────────────────────────
        // Daemon = JVM won't wait for this thread to finish on shutdown.
        // A thread pool (e.g. ExecutorService) would be cleaner at scale,
        // but for a single login flow a daemon thread is sufficient.
        Thread thread = new Thread(signupTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ── Back Navigation ───────────────────────────────────────────────────────

    /**
     * Called when the Back button is clicked.
     * Discards all form input and returns to the login screen.
     */
    @FXML
    private void handleBack() {
        NavigationService.navigateTo(ViewType.LOGIN);
    }
}
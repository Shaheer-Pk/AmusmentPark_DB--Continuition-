package com.amusementpark.controller;

import com.amusementpark.util.AlertHelper;
import com.amusementpark.dao.AuthDAO;
import com.amusementpark.model.User;
import com.amusementpark.navigation.PostLoginRouter;
import com.amusementpark.navigation.NavigationService;
import com.amusementpark.navigation.ViewType;
import com.amusementpark.session.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// AuthController — handles login form submission and signup button navigation.
//
// This controller is paired with login.fxml.
// signup.fxml has its own controller: SignupController.
//
// RESPONSIBILITIES:
//   - Validate login from input
//   - Call AuthDAO to authenticate credentials
//   - On success: initialise SessionManager, hand off to PostLoginRouter
//   - On failure: show error via AlertHelper, never reveal which field is wrong
//   - Navigate to signup screen (Customer or Vendor) when those buttons are clicked
//
// WHAT THIS CONTROLLER DOES NOT DO:
//   - No password hashing (AuthDAO owns that via BCrypt)
//   - No routing decisions (PostLoginRouter owns that)
//   - No direct database queries (AuthDAO responsibility)
// ─────────────────────────────────────────────────────────────────────────────
public class AuthController {

    // ── FXML Field Injection ──────────────────────────────────────────────────
    // JavaFX reads the fx:id attributes in login.fxml and injects the matching
    // live node objects into these fields before initialize() is called.

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;

    // ── DAO Dependencies ──────────────────────────────────────────────────────

    private final AuthDAO authDAO = new AuthDAO();

    // ── Login Handler ─────────────────────────────────────────────────────────

    /**
     * Called when the Login button is clicked (fx:onAction in login.fxml).
     *
     * FLOW:
     *   1. Validate fields are not empty.
     *   2. Call AuthDAO.authenticate() — verifies email + BCrypt password.
     *   3. If null returned → show generic error, stop.
     *   4. Load roles and permissions from DB.
     *   5. Initialise SessionManager with user + roles + permissions.
     *   6. Hand off to PostLoginRouter to navigate to the correct dashboard.
     */
    @FXML
    private void handleLogin() {

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Input validation ──────────────────────────────────────────────────
        if (email.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Missing Fields", "Please enter both email and password.");
            return;
        }

        try {
            // ── Step 1: Verify credentials ────────────────────────────────────
            User user = authDAO.authenticate(email, password);

            if (user == null) {
                // Intentionally vague — do not say which field is wrong.
                // Telling a user "that email doesn't exist" lets attackers
                // enumerate valid accounts.
                AlertHelper.showError("Login Failed", "Invalid email or password.");
                passwordField.clear();
                return;
            }

            // ── Step 2: Load roles and permissions ────────────────────────────
            Set<String> roles       = authDAO.loadRoles(user.getUserID());
            Set<String> permissions = authDAO.loadPermissions(user.getUserID());

            // ── Step 3: Initialise the session ────────────────────────────────
            // From this point forward, every controller in the app can call
            // SessionManager.getInstance().hasPermission("...") to gate access.
            SessionManager.getInstance().initSession(user, roles, permissions);

            // ── Step 4: Navigate to the correct dashboard ─────────────────────
            // PostLoginRouter reads the session roles and decides which panel
            // to land on. AuthController does not make that decision.
            PostLoginRouter.routeFromLogin();

        } catch (SQLException e) {
            AlertHelper.showError("Database Error", "Could not connect. Please try again.");
            e.printStackTrace();
        }
    }

    // ── Signup Navigation Handlers ────────────────────────────────────────────

    /**
     * Called when "Sign up as Customer" is clicked.
     * Navigates to signup.fxml and tells SignupController the role is Customer.
     */
    @FXML
    private void handleCustomerSignup() {
        SignupController ctrl =
            NavigationService.navigateToWithController(ViewType.SIGNUP);
        ctrl.initRole("Customer");
    }

    /**
     * Called when "Sign up as Vendor" is clicked.
     * Navigates to signup.fxml and tells SignupController the role is Vendor.
     */
    @FXML
    private void handleVendorSignup() {
        SignupController ctrl =
            NavigationService.navigateToWithController(ViewType.SIGNUP);
        ctrl.initRole("Vendor");
    }
}
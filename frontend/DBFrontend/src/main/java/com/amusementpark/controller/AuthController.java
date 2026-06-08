package com.amusementpark.controller;

import com.amusementpark.dao.AuthDAO;
import com.amusementpark.model.Card;
import com.amusementpark.model.User;
import com.amusementpark.navigation.NavigationService;
import com.amusementpark.navigation.PostLoginRouter;
import com.amusementpark.navigation.ViewType;
import com.amusementpark.session.SessionManager;
import com.amusementpark.util.AlertHelper;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// AuthController — handles login form submission and signup navigation.
//
// Paired with login.fxml. Signup has its own controller: SignupController.
//
// RESPONSIBILITIES:
//   - Validate form input (non-empty fields)
//   - Run the full login sequence off the FX thread in a Task
//   - On success: initialise SessionManager, hand off to PostLoginRouter
//   - On failure: show error via AlertHelper, re-enable form
//   - Navigate to signup screen when Customer/Vendor signup buttons clicked
//
// WHAT CHANGED FROM V1:
//   The entire login sequence (authenticate, loadRoles, loadPermissions,
//   loadCardDetails/loadStaffID) is now wrapped in a Task<Void> and runs
//   on a daemon thread. The FX Application Thread is never blocked.
//
//   V1 ran all DB calls — including BCrypt (100-300ms alone) — on the FX
//   thread. Any slow DB connection would freeze the UI. That is fixed here.
//
// THREADING MODEL:
//   Task runs on:  background thread (via new Thread(task).start())
//   SessionManager.initSession() called on: FX thread (via Platform.runLater)
//   PostLoginRouter.routeFromLogin() called on: FX thread (via Platform.runLater)
//   AlertHelper.showError() called on: FX thread (via Platform.runLater)
//
// WHAT THIS CONTROLLER DOES NOT DO:
//   - No password hashing (AuthDAO / BCrypt)
//   - No routing decisions (PostLoginRouter)
//   - No direct DB queries (AuthDAO)
// ─────────────────────────────────────────────────────────────────────────────
public class AuthController {

    // ── FXML Injection ────────────────────────────────────────────────────────

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;      // disabled during Task execution

    // ── DAO ───────────────────────────────────────────────────────────────────

    private final AuthDAO authDAO = new AuthDAO();

    // ── Login Handler ─────────────────────────────────────────────────────────

    /**
     * Called when the Login button is clicked (fx:onAction in login.fxml).
     *
     * FLOW:
     *   1. Validate fields non-empty — synchronous, on FX thread, no DB.
     *   2. Disable login button — prevents double-submit during Task execution.
     *   3. Spin up a background Task that runs:
     *        a. authenticate()      — BCrypt hash comparison + User load
     *        b. loadRoles()         — fetch role set
     *        c. loadPermissions()   — fetch permission union
     *        d. loadCardDetails()   — only if roles contains "Customer"
     *        e. loadStaffID()       — only if roles contains "Staff"
     *   4. On Task success: initSession() + routeFromLogin() on FX thread.
     *   5. On Task failure: show error, re-enable button on FX thread.
     */
    @FXML
    private void handleLogin() {

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Input validation — no DB needed, run on FX thread ─────────────────
        if (email.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Missing Fields", "Please enter both email and password.");
            return;
        }

        // ── Disable button — prevent double-submit while Task runs ─────────────
        loginButton.setDisable(true);

        // ── Build the login Task ───────────────────────────────────────────────
        // All DB work runs on a background thread. No FX thread blocking.
        // Local variables captured in the lambda must be effectively final.
        Task<Void> loginTask = new Task<>() {

            // These are populated inside call() and read in onSucceeded().
            // They live here so the success handler can reach them without
            // another DB call. Volatile not needed — Task guarantees that
            // call() completes-before onSucceeded fires.
            private User        user;
            private Set<String> roles;
            private Set<String> permissions;
            private Card        card    = null;  // null unless Customer
            private int         staffID = -1;    // -1 unless Staff

            @Override
            protected Void call() throws SQLException {

                // ── Step 1: Verify credentials ────────────────────────────────
                user = authDAO.authenticate(email, password);

                if (user == null) {
                    // Null signals bad credentials — handled in onFailed via
                    // a sentinel exception so the Task failure path shows the
                    // correct message without a separate boolean flag.
                    throw new InvalidCredentialsException();
                }

                // ── Step 2: Load roles and permissions ────────────────────────
                roles       = authDAO.loadRoles(user.getUserID());
                permissions = authDAO.loadPermissions(user.getUserID());

                // ── Step 3: Load Card — Customer accounts only ─────────────────
                // We check the raw roles Set here because SessionManager is not
                // yet initialised — session.hasRole() would return false for everything.
                if (roles.contains("Customer")) {
                    card = authDAO.loadCardDetails(user.getUserID());
                    // card == null here means the trigger didn't fire — data integrity
                    // issue. We don't crash; SessionManager stores null and purchase
                    // panels will show an appropriate error when getCard() returns null.
                }

                // ── Step 4: Load StaffID — Staff accounts only ────────────────
                if (roles.contains("Staff")) {
                    staffID = authDAO.loadStaffID(user.getUserID());
                    // staffID == -1 means no Staff row found for this UserID.
                    // That is a data integrity issue — Staff user with no Staff row.
                    // Stored as -1; operator panels check for this and handle gracefully.
                }

                return null;
            }

            @Override
            protected void succeeded() {
                // Back on the FX thread — safe to touch UI and SessionManager.
                SessionManager.getInstance().initSession(user, roles, permissions, card, staffID);
                PostLoginRouter.routeFromLogin();
                // Button stays disabled — we're navigating away from this screen.
            }

            @Override
            protected void failed() {
                // Back on the FX thread — safe to touch UI.
                Throwable ex = getException();

                if (ex instanceof InvalidCredentialsException) {
                    // Bad email or password — intentionally vague message.
                    AlertHelper.showError("Login Failed", "Invalid email or password.");
                    passwordField.clear();
                } else {
                    // Genuine DB or network error.
                    AlertHelper.showError("Database Error", "Could not connect. Please try again.");
                    ex.printStackTrace();
                }

                // Re-enable the button so the user can try again.
                loginButton.setDisable(false);
            }
        };

        // ── Run the Task on a daemon thread ────────────────────────────────────
        // Daemon = JVM won't wait for this thread to finish on shutdown.
        // A thread pool (e.g. ExecutorService) would be cleaner at scale,
        // but for a single login flow a daemon thread is sufficient.
        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ── Signup Navigation Handlers ────────────────────────────────────────────

    /**
     * "Sign up as Customer" button — navigates to signup.fxml, sets role.
     */
    @FXML
    private void handleCustomerSignup() {
        SignupController ctrl =
            NavigationService.navigateToWithController(ViewType.SIGNUP);
        ctrl.initRole("Customer");
    }

    /**
     * "Sign up as Vendor" button — navigates to signup.fxml, sets role.
     */
    @FXML
    private void handleVendorSignup() {
        SignupController ctrl =
            NavigationService.navigateToWithController(ViewType.SIGNUP);
        ctrl.initRole("Vendor");
    }

    // ── Sentinel Exception ────────────────────────────────────────────────────

    /**
     * Thrown inside the Task when authenticate() returns null (bad credentials).
     * Lets the onFailed() handler distinguish "wrong password" from a real
     * SQLException without a separate boolean flag or checked exception wrapper.
     *
     * Private and static — only meaningful inside this controller.
     */
    private static class InvalidCredentialsException extends RuntimeException {
        InvalidCredentialsException() {
            super("Invalid credentials");
        }
    }
}
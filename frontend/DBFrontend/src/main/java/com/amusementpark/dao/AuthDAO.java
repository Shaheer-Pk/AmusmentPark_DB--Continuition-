package com.amusementpark.dao;

import com.amusementpark.db.DatabaseConnection;
import com.amusementpark.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// AuthDAO — handles everything needed to authenticate a login attempt
//           and load the full session data afterward.
//
// THREE RESPONSIBILITIES, in the order they are called during login:
//
//   1. authenticate(email, rawPassword)
//      Finds the Login row, verifies the password with BCrypt.
//      Returns the User object if correct, null if not.
//
//   2. loadRoles(userID)
//      Fetches all role names assigned to this user.
//      e.g. { "Staff", "FinanceManager" }
//
//   3. loadPermissions(userID)
//      Fetches the UNION of all permissions across all the user's roles.
//      e.g. { "VIEW_REVENUE", "VIEW_LEDGER", "VIEW_PROFILE", "EDIT_PROFILE" }
//      This is what SessionManager stores and what every controller queries.
//
// AuthController calls all three in sequence and hands the results to
// SessionManager.initSession() to complete the login.
//
// ─────────────────────────────────────────────────────────────────────────────
public class AuthDAO {

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Attempts to authenticate the given email and password.
     *
     * HOW IT WORKS:
     *   1. Look up the Login row by email — fetch the stored hash and UserID.
     *   2. Use BCrypt.checkpw() to compare the plain-text input against the hash.
     *      BCrypt extracts the salt from the stored hash automatically —
     *      you never store or manage the salt separately.
     *   3. If the password matches, load and return the full User object.
     *
     * WHAT IT RETURNS:
     *   The authenticated User object on success.
     *   null if the email doesn't exist OR the password is wrong.
     *   We intentionally give the same result for both cases — telling a user
     *   "that email doesn't exist" is an information leak that helps attackers
     *   enumerate valid accounts.
     *
     * @param email       the email address from the login form
     * @param rawPassword the plain-text password from the login form
     * @return            authenticated User, or null if credentials are invalid
     */
    public User authenticate(String email, String rawPassword) throws SQLException {

        String sql = "SELECT l.UserID, l.Password "
                   + "FROM Login l "
                   + "WHERE l.Email = ?";

        try (PreparedStatement ps = DatabaseConnection.getActiveConn().prepareStatement(sql)) {       // A neat trick to close the 'ps' automatically after this try-block
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {        //ps.executeQuery returns a virtual resultSet table based on what we have executed so far
                if (!rs.next()) {
                    // No row found for this email — return null, not an exception.
                    // The controller shows a generic "Invalid credentials" message.
                    return null;
                }

                int    userID       = rs.getInt("UserID");
                String storedHash   = rs.getString("Password");

                // BCrypt.checkpw hashes rawPassword with the salt embedded in
                // storedHash and compares the result. Never do a plain string
                // comparison on passwords — BCrypt handles timing-safe comparison.
                if (!BCrypt.checkpw(rawPassword, storedHash)) {
                    return null;  // Wrong password — same response as wrong email.
                }

                // Credentials valid — load and return the full User object.
                return loadUser(DatabaseConnection.getActiveConn(), userID);
            }
        }
    }

    // ── Session Data Loading ──────────────────────────────────────────────────

    /**
     * Loads all role names assigned to the given user.
     * Called immediately after authenticate() succeeds.
     *
     * Example result: { "Staff", "FinanceManager" }
     *
     * @param userID the authenticated user's ID
     * @return set of role name strings — empty set if none assigned
     */
    public Set<String> loadRoles(int userID) throws SQLException {

        String sql = "SELECT r.RoleName "
                   + "FROM UserRole ur "
                   + "JOIN Role r ON ur.RoleID = r.RoleID "
                   + "WHERE ur.UserID = ?";

        Set<String> roles = new HashSet<>();

        try (PreparedStatement ps = DatabaseConnection.getActiveConn().prepareStatement(sql)) {   
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("RoleName"));
                }
            }
        }
        return roles;
    }

    /**
     * Loads the effective permission set for the given user.
     * This is the UNION of all permissions across ALL of the user's roles.
     * Called immediately after loadRoles() succeeds.
     *
     * HOW THE UNION WORKS IN SQL:
     *   We join UserRole → RolePermission → Permission in one query.
     *   If the user has roles Staff and FinanceManager, we get every
     *   permission mapped to either role in a single result set.
     *   DISTINCT ensures duplicates are collapsed — if two roles share
     *   VIEW_PROFILE, it only appears once in the result.
     *
     * Example result: { "VIEW_REVENUE", "VIEW_LEDGER", "VIEW_PROFILE", "EDIT_PROFILE" }
     *
     * @param userID the authenticated user's ID
     * @return set of permission name strings — empty set if none assigned
     */
    public Set<String> loadPermissions(int userID) throws SQLException {

        // Walk the full RBAC chain:
        // User → UserRole → RolePermission → Permission
        String sql = "SELECT DISTINCT p.PermissionName "
                   + "FROM UserRole ur "
                   + "JOIN RolePermission rp ON ur.RoleID = rp.RoleID "
                   + "JOIN Permission p      ON rp.PermissionID = p.PermissionID "
                   + "WHERE ur.UserID = ?";

        Set<String> permissions = new HashSet<>();

        try (PreparedStatement ps = DatabaseConnection.getActiveConn().prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("PermissionName"));
                }
            }
        }
        return permissions;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Loads the full User object from the User table by ID.
     * Called internally after password verification succeeds.
     */
    private User loadUser(Connection conn, int userID) throws SQLException {

        String sql = "SELECT UserID, FirstName, LastName, PhoneNumber, DateOfBirth "
                   + "FROM User WHERE UserID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("UserID"),
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getString("PhoneNumber"),
                        rs.getDate("DateOfBirth").toLocalDate()  // java.sql.Date → LocalDate
                    );
                }
                // UserID came from the Login table — if User row is missing,
                // the database has an integrity violation. Fail loudly.
                throw new SQLException("User row not found for UserID: " + userID);
            }
        }
    }
}
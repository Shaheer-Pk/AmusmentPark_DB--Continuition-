package com.amusementpark.dao;

import com.amusementpark.db.DatabaseConnection;
import com.amusementpark.model.Card;
import com.amusementpark.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// AuthDAO — handles everything needed to authenticate a login attempt
//           and load the full session data afterward.
//
// FIVE RESPONSIBILITIES, in the order they are called during login:
//
//   1. authenticate(email, rawPassword)
//      Finds the Login row, verifies the password with BCrypt.
//      Returns the User object if correct, null if not.
//
//   2. loadRoles(userID)
//      Fetches all role names assigned to this user.
//      e.g. { "Staff", "RideOperator" }
//
//   3. loadPermissions(userID)
//      Fetches the UNION of all permissions across all the user's roles.
//      e.g. { "VIEW_REVENUE", "VIEW_LEDGER", "VIEW_PROFILE", "EDIT_PROFILE" }
//      This is what SessionManager stores and what every controller queries.
//
//   4. loadCardDetails(userID)          — Customer only
//      Fetches the Card row for this user. Called only when roles contains
//      "Customer". Returns null if no Card row found (should never happen
//      for a Customer due to trg_create_card trigger, but we handle it safely).
//
//   5. loadStaffID(userID)             — Staff only
//      Fetches the StaffID from the Staff table for this user.
//      Called only when roles contains "Staff". Returns -1 if not found.
//      StaffID is needed by operator panels (Rides, Cinema, Bowling) to
//      query assigned records — stored in SessionManager so no per-panel
//      DB lookup is ever needed.
//
// CONNECTION STRATEGY — FIXED:
//   Every method acquires ONE connection via getActiveConn(), uses it for the
//   full operation, and closes it in the try-with-resources block.
//   The previous version called getActiveConn() inside a PreparedStatement
//   try-with-resources — closing the PS but NOT the connection. Every login
//   call leaked connections back to HikariCP. That is fixed here.
//
// THREADING:
//   All methods are called from a background Task in AuthController.
//   Never call these on the FX Application Thread.
// ─────────────────────────────────────────────────────────────────────────────
public class AuthDAO {

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Attempts to authenticate the given email and password.
     *
     * HOW IT WORKS:
     *   1. Look up the Login row by email — fetch stored hash and UserID.
     *   2. BCrypt.checkpw() compares plain-text input against the stored hash.
     *      BCrypt extracts the salt from the stored hash automatically.
     *   3. If password matches, load and return the full User object.
     *
     * WHAT IT RETURNS:
     *   The authenticated User object on success.
     *   null if email doesn't exist OR password is wrong.
     *   Intentionally identical result for both — avoids account enumeration.
     *
     * CONNECTION: One connection for the whole method — login query + user load.
     * Closed by try-with-resources on the Connection, not just the Statement.
     */
    public User authenticate(String email, String rawPassword) throws SQLException {

        String loginSql = "SELECT l.UserID, l.Password "
                        + "FROM Login l "
                        + "WHERE l.Email = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(loginSql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;  // No account with this email.

                int    userID     = rs.getInt("UserID");
                String storedHash = rs.getString("Password");

                if (!BCrypt.checkpw(rawPassword, storedHash)) return null;  // Wrong password.

                // Credentials valid — load the full User on the same connection.
                return loadUser(conn, userID);
            }
        }
    }

    // ── Session Data Loading ──────────────────────────────────────────────────

    /**
     * Loads all role names assigned to the given user.
     * Called immediately after authenticate() succeeds.
     *
     * Example result: { "Staff", "RideOperator" }
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

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
     * UNION of all permissions across ALL of the user's roles.
     * Called immediately after loadRoles() succeeds.
     *
     * DISTINCT collapses duplicates — if two roles share VIEW_PROFILE,
     * it appears only once in the result set.
     *
     * Example result: { "VIEW_REVENUE", "VIEW_LEDGER", "EDIT_PROFILE" }
     *
     * @param userID the authenticated user's ID
     * @return set of permission name strings — empty set if none assigned
     */
    public Set<String> loadPermissions(int userID) throws SQLException {

        String sql = "SELECT DISTINCT p.PermissionName "
                   + "FROM UserRole ur "
                   + "JOIN RolePermission rp ON ur.RoleID = rp.RoleID "
                   + "JOIN Permission p      ON rp.PermissionID = p.PermissionID "
                   + "WHERE ur.UserID = ?";

        Set<String> permissions = new HashSet<>();

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("PermissionName"));
                }
            }
        }
        return permissions;
    }

    /**
     * Loads the Card row for the given user.
     *
     * WHY THIS EXISTS:
     *   Every purchase stored procedure (PurchaseRide, PurchaseTicket,
     *   StartBowlingSession) requires a CardID. Rather than querying Card
     *   in every panel's purchase handler, we load it once at login and
     *   store it in SessionManager. Controllers call session.getCard().getCardID().
     *
     * WHEN TO CALL:
     *   Only when roles.contains("Customer") in AuthController.
     *   Staff and Vendor never get a Card loaded into session.
     *
     * WHAT IF NO CARD ROW EXISTS:
     *   For a Customer this should never happen — trg_create_card fires on
     *   every User insert and creates the Card row automatically. But we
     *   return null safely rather than throwing — AuthController handles it.
     *
     * @param userID the authenticated customer's UserID
     * @return Card object if found, null if no card exists for this user
     */
    public Card loadCardDetails(int userID) throws SQLException {

        String sql = "SELECT CardID, Balance, LoyaltyPoints, IsActive "
                   + "FROM Card WHERE UserID = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Card(
                        rs.getInt("CardID"),
                        rs.getBigDecimal("Balance"),
                        rs.getInt("LoyaltyPoints"),
                        rs.getBoolean("IsActive")
                    );
                }
                return null;  // No card found — caller handles this case.
            }
        }
    }

    /**
     * Loads the StaffID for the given user from the Staff table.
     *
     * WHY THIS EXISTS:
     *   Operator panels (Rides, Cinema, Bowling) need StaffID to query
     *   records assigned to the logged-in operator. Loaded once at login,
     *   stored in SessionManager. Controllers call session.getStaffID().
     *   No per-panel Staff table lookup ever needed.
     *
     * WHEN TO CALL:
     *   Only when roles.contains("Staff") in AuthController.
     *   Customers and Vendors never have a Staff row.
     *
     * @param userID the authenticated staff member's UserID
     * @return the StaffID, or -1 if no Staff row found for this UserID
     */
    public int loadStaffID(int userID) throws SQLException {

        String sql = "SELECT StaffID FROM Staff WHERE UserID = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("StaffID");
                return -1;  // No Staff row — caller stores -1 in session.
            }
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Loads the full User object from the User table by ID.
     * Called internally by authenticate() on the same connection —
     * avoids opening a second connection just to load the user row.
     *
     * NOTE: Uses rs.getDate("DateOfBirth").toLocalDate() for the date field.
     * This is acceptable here because we're reading from MySQL (which stores
     * DATE as yyyy-MM-dd) and converting immediately to LocalDate. We never
     * store a java.sql.Date in the model. See UserDAO for why we use
     * ps.setObject() with LocalDate on writes.
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
                        rs.getDate("DateOfBirth").toLocalDate()
                    );
                }
                // UserID came from Login table — User row missing = integrity violation.
                throw new SQLException("User row not found for UserID: " + userID);
            }
        }
    }
}
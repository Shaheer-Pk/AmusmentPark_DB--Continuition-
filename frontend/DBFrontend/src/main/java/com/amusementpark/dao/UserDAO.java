package com.amusementpark.dao;

import com.amusementpark.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;

// ─────────────────────────────────────────────────────────────────────────────
// UserDAO — handles all database writes for creating a new user account.
//
// RESPONSIBILITIES:
//   - Insert a new User row
//   - Insert the matching Login row (with hashed password)
//   - Assign the correct Role (Customer or Vendor)
//   All three happen in ONE transaction. If anything fails, everything
//   rolls back — no partial/orphaned records ever reach the database.
//
// WHAT THIS CLASS DOES NOT DO:
//   - Authentication (that is AuthDAO's job)
//   - Password comparison (BCrypt.checkpw lives in AuthDAO)
//   - Any UI interaction
//
// SOME NOTES RELATED TO USERID:
//   - The userID generated may not be in a proper sequence e.g 15, 16, 17, ...
//   - This is a design choice and its okay for userID to look like 15, 18, 19, ....
//   - This is currently caused by duplicate email case in SignupController
//   - For more detail see the structure of SignupController and search for
//   -  'e.getErrorCode == 1062' using ctrl + f
//
// SOME NOTES RELATED TO DATE FORMATTING:
//   - DONT USE ps.setDate as it enforces the legacy java.sql.Date date format (which ruins the POJO user.java)
//   - DONT USE JavaFX DatePicker as it uses system's regional formatting (for America its mm/dd/yyyy for EU its yyyy/mm/dd)
//   - instead use ps.setObject and pass date of format type LocalDate into it
//   - Just use localDate everywhere when forming POJO and when storying in db as MySQL and
//   - localDate both enforce strict ISO-8601 format (YYYY-MM-DD)
//   - This ensures that the model User.java when used will not break in between as it uses java.time.LocalDate
// ─────────────────────────────────────────────────────────────────────────────
public class UserDAO {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates a complete user account in a single atomic transaction:
     *   1. Insert into User table
     *   2. Insert into Login table (password is hashed here before storage)
     *   3. Assign the role ('Customer' or 'Vendor') in UserRole table
     *
     * The Card row is created automatically by the database trigger
     * trg_create_card — we do not insert it manually here.
     *
     * @param firstName   user's first name
     * @param lastName    user's last name
     * @param phone       user's phone number
     * @param dob         user's date of birth
     * @param email       login email — must be unique
     * @param rawPassword plain-text password — hashed before storing
     * @param roleName    either "Customer" or "Vendor"
     * @return            returns userID to be utilized when creating a specialized staff account in staff_panel
     * @throws SQLException if the email already exists or any DB error occurs
     */
    public int createUser(String firstName, String lastName, String phone,
                           LocalDate dob, String email, String rawPassword,
                           String roleName) throws SQLException {

        Connection conn = DatabaseConnection.getActiveConn();

        // We manage the transaction manually so all three inserts are atomic.
        // autoCommit = false means nothing is written to disk until commit().
        // If anything throws before commit, rollback() undoes every insert.
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            // Step 1: Insert the User row, get back the generated UserID.
            int userID = insertUser(conn, firstName, lastName, phone, dob);

            // Step 2: Hash the password and insert the Login row.
            // BCrypt.hashpw generates a random salt and hashes in one call.
            // Plain-text password is never stored — only this hash.
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
            insertLogin(conn, email, hashedPassword, userID);

            // Step 3: Assign the role by name — never by hardcoded ID.
            assignRole(conn, userID, roleName);

            // All three succeeded — persist to disk.
            conn.commit();

            return userID;      // Ignored by signUpController but utilized by Staff-setup Panel

        } catch (SQLException e) {
            // Something failed — undo every insert made in this block.
            conn.rollback();
            // Re-throw so AuthController can catch it and show the right message.
            throw e;

        } finally {
            // Always restore autoCommit. We share one connection across the app —
            // leaving it false would silently break every other DAO after this.
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Inserts a User row and returns the auto-generated UserID.
     * RETURN_GENERATED_KEYS tells JDBC to capture the auto-increment value
     * so we can use it in the Login and UserRole inserts immediately after.
     */
    private int insertUser(Connection conn, String firstName, String lastName,
                           String phone, LocalDate dob) throws SQLException {

        String sql = "INSERT INTO User (FirstName, LastName, PhoneNumber, DateOfBirth) "
                   + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, phone);
            ps.setObject(4, dob);   // 
            ps.executeUpdate();                    //  
                                                  //  
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("User insert succeeded but no UserID was returned.");
            }
        }
    }

    /**
     * Inserts a Login row.
     * Email has a UNIQUE constraint in the schema — if it already exists,
     * MySQL throws SQLException error code 1062 (duplicate entry).
     * AuthController catches that specific code and shows "Email already registered."
     */
    private void insertLogin(Connection conn, String email,
                             String hashedPassword, int userID) throws SQLException {

        String sql = "INSERT INTO Login (Email, Password, UserID) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashedPassword);
            ps.setInt(3, userID);
            ps.executeUpdate();
        }
    }

    /**
     * Assigns a role to the new user by name using a subquery.
     * We never hardcode RoleIDs — if seed data is re-run or row order changes,
     * this still finds the correct role. Zero rows inserted means the roleName
     * doesn't exist in the Role table — that is a developer error, not user error.
     */
    private void assignRole(Connection conn, int userID, String roleName) throws SQLException {

        String sql = "INSERT INTO UserRole (UserID, RoleID) "
                   + "SELECT ?, RoleID FROM Role WHERE RoleName = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setString(2, roleName);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Role not found in database: " + roleName);
            }
        }
    }
}
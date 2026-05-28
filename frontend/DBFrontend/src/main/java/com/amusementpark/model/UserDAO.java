package com.amusementpark.model;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDAO extends BaseDAO<UserAccount> {

    /**
     * Secures and executes user authentication using BCrypt verification.
     * Joins the Customer table to build a rich user context object upon login success.
     */
    public UserAccount authenticate(String email, String plainTextPassword) throws SQLException {
        String sql = "SELECT l.LoginID, l.Email, l.Password, l.isAdmin, l.CustomerID, l.Created_at, " +
                     "c.First_Name, c.Last_Name " +
                     "FROM Login l " +
                     "LEFT JOIN Customer c ON l.CustomerID = c.CustomerID " +
                     "WHERE l.Email = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("Password");
                    
                    // Verify plaintext entry against secure database hash
                    if (BCrypt.checkpw(plainTextPassword, storedHash)) {
                        Integer custIdObj = rs.getObject("CustomerID") != null ? rs.getInt("CustomerID") : null;
                        
                        LocalDateTime crAt = rs.getTimestamp("Created_at") != null 
                            ? rs.getTimestamp("Created_at").toLocalDateTime() 
                            : null;

                        return new UserAccount(
                            rs.getInt("LoginID"),
                            rs.getString("Email"),
                            rs.getBoolean("isAdmin"),
                            custIdObj,
                            rs.getString("First_Name"),
                            rs.getString("Last_Name"),
                            crAt
                        );
                    }
                }
            }
        }
        return null; // Authentication failure
    }

    /**
     * Execution pipeline for atomic registration across Customer and Login tables.
     * Leverages manual transaction rollbacks to preserve database consistency on errors.
     */
    public boolean registerUser(String firstName, String lastName, LocalDate dob, 
                                String email, String plainTextPassword, boolean isAdmin) throws SQLException {
        
        Connection conn = null;
        PreparedStatement custStmt = null;
        PreparedStatement loginStmt = null;
        ResultSet rsKeys = null;

        String customerSql = "INSERT INTO Customer (First_Name, Last_Name, Type, Date_of_Birth) VALUES (?, ?, 'Standard', ?)";
        String loginSql = "INSERT INTO Login (Email, Password, isAdmin, CustomerID) VALUES (?, ?, ?, ?)";

        try {
            conn = getConnection();
            // Engage manual transactional mode
            conn.setAutoCommit(false);

            // Step A: Build human profile entry inside Customer Table
            custStmt = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS);
            custStmt.setString(1, firstName);
            custStmt.setString(2, lastName);
            custStmt.setDate(3, java.sql.Date.valueOf(dob));
            custStmt.executeUpdate();

            // Extract the newly minted CustomerID auto-incremented key
            int generatedCustomerId = -1;
            rsKeys = custStmt.getGeneratedKeys();
            if (rsKeys.next()) {
                generatedCustomerId = rsKeys.getInt(1);
            }

            if (generatedCustomerId == -1) {
                throw new SQLException("Failed to resolve auto-incremented CustomerID primary key.");
            }

            // NOTE: The automated database trigger 'after_customer_insert' runs right here,
            // effortlessly provisioning their associated Card record.

            // Step B: Hash the raw password using BCrypt
            String encryptedPassword = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));

            // Step C: Construct access privileges inside Login Table bound to the Customer ID
            loginStmt = conn.prepareStatement(loginSql);
            loginStmt.setString(1, email);
            loginStmt.setString(2, encryptedPassword);
            loginStmt.setBoolean(3, isAdmin);
            loginStmt.setInt(4, generatedCustomerId);
            loginStmt.executeUpdate();

            // Step D: Commit the full execution pipeline together
            conn.commit();
            return true;

        } catch (SQLException ex) {
            // Safety fallback: Undo any processing if any step fails (e.g., duplicate email)
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Transaction rollback error: " + rollbackEx.getMessage());
                }
            }
            throw ex; // Pass error up to the UI Layer
        } finally {
            // Clean resources thoroughly
            if (rsKeys != null) rsKeys.close();
            if (custStmt != null) custStmt.close();
            if (loginStmt != null) loginStmt.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    @Override
    protected UserAccount mapRow(ResultSet rs) throws SQLException {
        // Implemented to satisfy generic BaseDAO definitions if utility queries are called
        Integer custIdObj = rs.getObject("CustomerID") != null ? rs.getInt("CustomerID") : null;
        LocalDateTime crAt = rs.getTimestamp("Created_at") != null ? rs.getTimestamp("Created_at").toLocalDateTime() : null;
        return new UserAccount(
            rs.getInt("LoginID"),
            rs.getString("Email"),
            rs.getBoolean("isAdmin"),
            custIdObj,
            null, // First Name not present in generic select * queries
            null, // Last Name not present in generic select * queries
            crAt
        );
    }
}
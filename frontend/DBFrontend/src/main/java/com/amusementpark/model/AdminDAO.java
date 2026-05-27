package com.amusementpark.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * AdminDAO: handles authentication against the Login table.
 * Passwords are stored plaintext per schema — flag this for hashing in production.
 */
public class AdminDAO extends BaseDAO<Admin> {

    /**
     * Authenticate by email + password.
     * Returns the Admin object on success, null on failure.
     */
    public Admin authenticate(String email, String password) throws SQLException {
        String sql = "SELECT * FROM Login WHERE Email = ? AND Password = ?";
        List<Admin> results = executeQuery(sql, email, password);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Fetch all admin accounts (for admin management if needed later).
     */
    public List<Admin> findAll() throws SQLException {
        return executeQuery("SELECT * FROM Login ORDER BY Name");
    }

    @Override
    protected Admin mapRow(ResultSet rs) throws SQLException {
        return new Admin(
            rs.getInt("LoginID"),
            rs.getString("Name"),
            rs.getString("Email"),
            rs.getString("Password"),
            rs.getTimestamp("Created_at") != null
                ? rs.getTimestamp("Created_at").toLocalDateTime()
                : null
        );
    }
}

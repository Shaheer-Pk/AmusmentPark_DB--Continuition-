package com.amusementpark.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thread-safe singleton for MySQL connection.
 * Change DB_URL / USER / PASS to match your environment.
 */
public class DatabaseConnection {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/amusementparkdb";
    private static final String USER    = "root";
    private static final String PASS    = "Shkvt907!";

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Add mysql-connector-j to your classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to AmusementParkDB: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null || isConnectionDead()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private static boolean isConnectionDead() {
        try {
            return instance.connection == null || instance.connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    /** Call this on application shutdown. */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing DB connection: " + e.getMessage());
        }
    }
}

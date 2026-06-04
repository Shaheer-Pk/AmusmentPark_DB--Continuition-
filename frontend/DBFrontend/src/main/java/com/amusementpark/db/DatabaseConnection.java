package com.amusementpark.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

// ─────────────────────────────────────────────────────────────────────────────
// DBConfig  –  package-private, static-only config loader.
// Reads db.properties from the working directory at class-load time.
// Keys expected:  db.URL  |  db.USER  |  db.PASS
// ─────────────────────────────────────────────────────────────────────────────
class DBConfig {

    private static final Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            // Fail hard at startup — a silent miss here means every DB call
            // later will NPE with a confusing message. Better to die early.
            throw new ExceptionInInitializerError(
                "Cannot load db.properties from working directory: " + e.getMessage()
            );
        }
    }

    static String getUrl()      { return properties.getProperty("db.URL");  }
    static String getUser()     { return properties.getProperty("db.USER"); }
    static String getPassword() { return properties.getProperty("db.PASS"); }
}

// ─────────────────────────────────────────────────────────────────────────────
// DatabaseConnection  –  lazy-initialised, multi-thread-safe singleton.
//
// OLD version:
//   isClosed() alone does NOT detect stale / timed-out connections.
//   MySQL's wait_timeout can silently kill a connection that still reports
//   isClosed() == false.  We now use isValid(2) (2-second probe timeout)
//   which sends a lightweight ping to the server.
// 
// FIX from previous version:
//   Shifted to a connection pool setup using HikariCP
//   It handles the old version issue NATIVELY
//
//------------ Public Static Helper Method ------------------------
//---------------- getActiveConn() -------------------------------- 
//
// NOTES:
//   When executed for the very first time by thread one, it establishes the Hikari DataSource
//   according to the HikariConfig setup given below in CPConfig through 
//   Bill Pugh Lazy Initialization method.
// 
//   When called by thread two later onwards, the JVM sees that a HikariDataSource has already been
//   setup and provides a connection to thread 2 and can handle upto 10 concurrent threads
//   (defined in the config below)
// ─────────────────────────────────────────────────────────────────────────────
public class DatabaseConnection {

    private static final String DB_URL = DBConfig.getUrl();
    private static final String USER   = DBConfig.getUser();
    private static final String PASS   = DBConfig.getPassword();

    private DatabaseConnection() {}

    // (Bill Pugh holder pattern) — no synchronised block needed on every call
    private final static class ConnectionPoolHolder {

        // DataSource is now fully encapsulated and auto-initialized
        private static final HikariDataSource dataSource;

        static {
            try  {
 
                dataSource = new HikariDataSource(CPConfig());

                // Explicit driver load — good habit even with modern service-loader JDBC.
                Class.forName("com.mysql.cj.jdbc.Driver");

            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException("MySQL JDBC Driver not found — check classpath.", e);
            } 
        }
    }
    /** Call once on application shutdown from MainApp.stop(). */
    public static void close() {
        if (ConnectionPoolHolder.dataSource != null && !ConnectionPoolHolder.dataSource.isClosed()) {
            ConnectionPoolHolder.dataSource.close();
        }
    }


// ── Private Helpers ───────────────────────────────────────────────────────
    /**
     * Fast-path connection retrieval.
     * Referencing ConnectionPoolHolder.dataSource inherently triggers safe, 
     * lazy, thread-safe class loading handled entirely by the JVM.
     */
    public static Connection getActiveConn() throws SQLException {

        // Accessing the Bill Pugh holder guarantees safe, lazy startup execution
        // This line forces JVM to check if the inner holder class has been initialized
        // If initialized just return the dataSource connection from the connection pools (handled by HikariCP itself)
        // If not, then execute the static block within inner class that initializes the dataSource
        // and gives the 1 connection from tray of 10 connections

        if (ConnectionPoolHolder.dataSource == null || ConnectionPoolHolder.dataSource.isClosed()) {
            throw new SQLException("Database Connection is closed or un-initialzed");
        }

        return ConnectionPoolHolder.dataSource.getConnection();
    }

// ── Hikari Config ───────────────────────────────────────────────────────

    /**
     * The code to setup Hikari Connection Pools
     * To define the timeout duration, and number of connections to host
     * This makes sure that the Bill Pugh Holder model instance generated
     * Isn't just a SINGLE RAW DriverManager.getConnection
     * Which each thread can use and trample on each other
     * Rather its a connection pool with a proper manager
     * who allocates connections to the threads requesting it
     * 
     * Changed to package private so the inner Holder class
     * can safely evaluate it at class-load time
     */
    static HikariConfig CPConfig() {
        HikariConfig config = new HikariConfig();

        // Establishing the connection to the jdbc (see DBConfig class for more info above)
        config.setJdbcUrl(DB_URL);
        config.setUsername(USER);
        config.setPassword(PASS);

        // Establishing the number of connection pools
        config.setMaximumPoolSize(10);

        /**
         * The reason why idleTimeout is higher than ConnectionTimeout is because when an
         * active connection comes demanding a connection, HikariCP will instantly hand it
         * an idle connection from the tray which takes less than a micro-second
         * IdleTimeout is for cases when the tray isnt full and there is no activity
         * so just free up connection and save usage
         */
        config.setConnectionTimeout(20000);     // 20 seconds wait limit
        config.setIdleTimeout(120000);                // 2 minutes inactivity window
        config.setMaxLifetime(1800000);                 // 30 minutes absolute age expiration
        
        return config;
    }
}
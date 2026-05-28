package com.amusementpark;

import com.amusementpark.model.UserAccount;

/**
 * SessionManager: Thread-safe singleton that tracks the authenticated user
 * across all active UI controllers.
 */
public class SessionManager {

    private static SessionManager instance;
    private UserAccount currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(UserAccount user) {
        this.currentUser = user;
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }
}
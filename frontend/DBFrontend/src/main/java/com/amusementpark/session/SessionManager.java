package com.amusementpark.session;

import com.amusementpark.model.User;
import com.amusementpark.util.AlertHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// SessionManager  –  single source of truth for the authenticated session.
//
// DESIGN RULES (from RBAC architecture notes):
//   1. Stores:  current User  |  assigned role names  |  effective permissions
//   2. Permissions are the UNION of all permissions across all assigned roles.
//   3. Controllers ONLY call hasPermission().  They never inspect role names.
//   4. This class is the gatekeeper — nothing else in the app decides access.
//
// LIFECYCLE:
//   Login  → AuthController calls SessionManager.initSession(...)
//   Logout → DashboardController calls SessionManager.clearSession()
//   Any controller → SessionManager.getInstance().hasPermission("VIEW_REVENUE")
//
// THREAD SAFETY:
//   JavaFX is single-threaded on the FX thread.  All UI interactions hit this
//   class from the same thread.  The singleton itself is initialised once at
//   class load (Bill Pugh holder pattern) — no synchronised block needed
//   on every call.
// ─────────────────────────────────────────────────────────────────────────────
public final class SessionManager {

    // ── Singleton ────────────────────────────────────────────────────────────

    private SessionManager() {}

    private static final class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    // ── Session State ─────────────────────────────────────────────────────────

    private User           currentUser;
    private Set<String>    roles;        // e.g. { "Staff", "FinanceManager" }
    private Set<String>    permissions;  // union of all role permissions

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called by AuthController immediately after successful login.
     *
     * @param user        the authenticated User object
     * @param roles       role names assigned to this user
     * @param permissions effective permission names (union across all roles)
     */
    public void initSession(User user, Set<String> roles, Set<String> permissions) {
        if (user == null) {
            // AlertHelper message to show to the user while the exception for developer
            // Hints an error in AuthController.java as sessions are initialized over there
            AlertHelper.showError("Invalid Session", "Session cannot be initialized, please report to some admin");
            throw new IllegalArgumentException("Cannot initialise session with a null user.");
        }
        this.currentUser = user;
        this.roles       = Collections.unmodifiableSet(new HashSet<>(roles));
        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
    }

    /**
     * Wipes all session data.
     * Call this on logout or on any security fault.
     */
    public void clearSession() {
        currentUser = null;
        roles       = null;
        permissions = null;
    }

    // ── Core RBAC Gate ────────────────────────────────────────────────────────

    /**
     * THE method every controller must use for access control.
     *
     * Usage:
     *   if (SessionManager.getInstance().hasPermission("DELETE_STAFF")) { ... }
     *
     * Returns false (not an exception) when no session is active — safe to call
     * during any UI lifecycle event without null-checking first.
     */
    public boolean hasPermission(String permissionName) {
        if (permissions == null || permissionName == null) return false;
        return permissions.contains(permissionName);
    }

    /**
     * Convenience: check multiple permissions at once.
     * Returns true only if the user holds ALL listed permissions.
     *
     * Usage:
     *   if (hasAllPermissions("VIEW_REVENUE", "EXPORT_REPORTS")) { ... }
     */
    public boolean hasAllPermissions(String... permissionNames) {
        if (permissions == null) return false;
        for (String p : permissionNames) {
            if (!permissions.contains(p)) return false;
        }
        return true;
    }

    /**
     * Convenience: check if the user holds ANY of the listed permissions.
     *
     * Usage:
     *   if (hasAnyPermission("MANAGE_RIDES", "UPDATE_RIDE_STATUS")) { ... }
     */
    public boolean hasAnyPermission(String... permissionNames) {
        if (permissions == null) return false;
        for (String p : permissionNames) {
            if (permissions.contains(p)) return true;
        }
        return false;
    }

    // ── Role Query (use sparingly — prefer hasPermission) ────────────────────

    /**
     * Role check — provided only for edge cases like "show Staff nav menu".
     * Do NOT use this to gate individual features.  That's what permissions are for.
     *
     * Legitimate uses:
     *   - Deciding which home dashboard FXML to load after login.
     *   - Showing/hiding entire navigation sections.
     */
    public boolean hasRole(String roleName) {
        if (roles == null || roleName == null) return false;
        return roles.contains(roleName);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /**
     * Returns the authenticated User, or null if no session is active.
     * Always null-check the result before use in controllers.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns an unmodifiable snapshot of the user's role names.
     * Returns an empty set (not null) when no session is active.
     */
    public Set<String> getRoles() {
        return roles != null ? roles : Collections.emptySet();
    }

    /**
     * Returns an unmodifiable snapshot of the user's effective permissions.
     * Returns an empty set (not null) when no session is active.
     */
    public Set<String> getPermissions() {
        return permissions != null ? permissions : Collections.emptySet();
    }

    /**
     * Returns true if a session is currently active.
     * Useful for guards in MainApp navigation.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
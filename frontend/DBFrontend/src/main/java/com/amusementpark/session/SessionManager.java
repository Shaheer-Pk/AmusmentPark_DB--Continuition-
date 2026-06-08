package com.amusementpark.session;

import com.amusementpark.model.Card;
import com.amusementpark.model.User;
import com.amusementpark.util.AlertHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// SessionManager  —  single source of truth for the authenticated session.
//
// DESIGN RULES:
//   1. Stores: User | roles | permissions | Card (Customer only) | staffID (Staff only)
//   2. Permissions are the UNION of all permissions across all assigned roles.
//   3. Controllers ONLY call hasPermission(). They never inspect role names
//      for feature gating — roles are only used for nav/dashboard routing.
//   4. Card is null for non-Customer users. Always null-check before use.
//   5. staffID is -1 for non-Staff users. Check staffID != -1 before use.
//
// WHAT CHANGED FROM V1:
//   - Added Card field — loaded at login for Customers, null for everyone else.
//     Eliminates per-panel card DB lookups. All purchase handlers call
//     session.getCard().getCardID() instead of querying the DB themselves.
//   - Added staffID field — loaded at login for Staff, -1 for everyone else.
//     Eliminates per-panel Staff table lookups in Rides, Cinema, Bowling panels.
//   - initSession() now takes Card and int staffID as additional parameters.
//   - clearSession() wipes Card and staffID alongside existing fields.
//
// LIFECYCLE:
//   Login  → AuthController calls initSession(user, roles, permissions, card, staffID)
//   Logout → DashboardController calls clearSession()
//   Any controller → SessionManager.getInstance().hasPermission("VIEW_REVENUE")
//   Purchase panel → SessionManager.getInstance().getCard().getCardID()
//   Operator panel → SessionManager.getInstance().getStaffID()
//
// THREAD SAFETY:
//   All UI interactions hit this class from the FX Application Thread.
//   initSession() is called on the FX thread after the background Task completes
//   (via Platform.runLater in AuthController). Singleton uses Bill Pugh holder
//   pattern — no synchronised block needed on every getInstance() call.
// ─────────────────────────────────────────────────────────────────────────────
public final class SessionManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private SessionManager() {}

    private static final class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    // ── Session State ─────────────────────────────────────────────────────────

    private User        currentUser;
    private Set<String> roles;           // e.g. { "Staff", "RideOperator" }
    private Set<String> permissions;     // union of all role permissions
    private Card        card;            // null for Staff/Vendor/Admin
    private int         staffID = -1;   // -1 for Customer/Vendor

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called by AuthController (via Platform.runLater) after the login Task
     * successfully completes all DB operations.
     *
     * @param user        the authenticated User object
     * @param roles       role names assigned to this user
     * @param permissions effective permission names (union across all roles)
     * @param card        Card object if user is a Customer, null otherwise
     * @param staffID     StaffID if user is Staff, -1 otherwise
     */
    public void initSession(User user, Set<String> roles, Set<String> permissions,
                            Card card, int staffID) {
        if (user == null) {
            AlertHelper.showError("Invalid Session",
                "Session cannot be initialised. Please report to an admin.");
            throw new IllegalArgumentException("Cannot initialise session with a null user.");
        }
        this.currentUser = user;
        this.roles       = Collections.unmodifiableSet(new HashSet<>(roles));
        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
        this.card        = card;       // null is valid — means non-Customer
        this.staffID     = staffID;    // -1 is valid — means non-Staff
    }

    /**
     * Wipes all session data.
     * Call this on logout or on any security fault.
     */
    public void clearSession() {
        currentUser = null;
        roles       = null;
        permissions = null;
        card        = null;
        staffID     = -1;
    }

    // ── Core RBAC Gate ────────────────────────────────────────────────────────

    /**
     * THE method every controller must use for feature access control.
     *
     * Usage:
     *   if (SessionManager.getInstance().hasPermission("DELETE_RIDE")) { ... }
     *
     * Returns false (not an exception) when no session is active — safe to call
     * during any UI lifecycle event without null-checking first.
     */
    public boolean hasPermission(String permissionName) {
        if (permissions == null || permissionName == null) return false;
        return permissions.contains(permissionName);
    }

    /**
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
     * Returns true if the user holds ANY of the listed permissions.
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

    // ── Role Query (use sparingly — prefer hasPermission) ─────────────────────

    /**
     * Role check — for edge cases like dashboard routing and nav menu visibility.
     * Do NOT use this to gate individual features. That is hasPermission()'s job.
     *
     * Legitimate uses:
     *   - PostLoginRouter deciding which dashboard FXML to load.
     *   - Showing/hiding entire navigation sections (Staff nav vs Customer nav).
     */
    public boolean hasRole(String roleName) {
        if (roles == null || roleName == null) return false;
        return roles.contains(roleName);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the authenticated User, or null if no session is active. */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns the customer's Card loaded at login.
     * NULL for Staff, Vendor, and Admin users — always null-check before use.
     *
     * Usage in purchase panels:
     *   Card card = SessionManager.getInstance().getCard();
     *   if (card == null) { // show error — should never happen for Customer }
     *   int cardID = card.getCardID();
     */
    public Card getCard() {
        return card;
    }

    /**
     * Returns the StaffID loaded at login.
     * Returns -1 for Customer and Vendor users — check staffID != -1 before use.
     *
     * Usage in operator panels:
     *   int staffID = SessionManager.getInstance().getStaffID();
     *   if (staffID == -1) { // not a staff member — shouldn't be on this panel }
     */
    public int getStaffID() {
        return staffID;
    }

    /** Returns an unmodifiable snapshot of role names. Empty set if no session. */
    public Set<String> getRoles() {
        return roles != null ? roles : Collections.emptySet();
    }

    /** Returns an unmodifiable snapshot of effective permissions. Empty set if no session. */
    public Set<String> getPermissions() {
        return permissions != null ? permissions : Collections.emptySet();
    }

    /** Returns true if a session is currently active. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
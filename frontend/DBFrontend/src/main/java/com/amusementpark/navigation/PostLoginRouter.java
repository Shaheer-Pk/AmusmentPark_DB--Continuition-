package com.amusementpark.navigation;

import com.amusementpark.session.SessionManager;
import com.amusementpark.controller.DashboardController;

// ─────────────────────────────────────────────────────────────────────────────
// PostLoginRouter — decides which panel to load first after a successful login.
//
// WHY THIS CLASS EXISTS INSTEAD OF PUTTING LOGIC IN AuthController:
//   As this project scales with time, 
//   routing after login will be needed from more than one place.  
//   If this logic lived in AuthController, any other class that needs to
//   trigger a "go to the right panel for this user" would have to call into
//   AuthController — a class it has no business depending on.
//   PostLoginRouter is a single, reusable home for that decision.
//
// HOW IT WORKS:
//   After SessionManager.initSession() is called, this class reads the
//   session's roles to decide the default landing panel, then uses
//   NavigationService to navigate to DASHBOARD and load that panel into it.
//
// ROUTING PRIORITY (highest to lowest):
//   Admin       → PANEL_HOME  (admin sees everything, home is the right start)
//   Staff       → PANEL_HOME  (staff home shows relevant stats for their roles)
//   Vendor      → PANEL_MY_STALL
//   Customer    → PANEL_RIDES (most common first action for a customer)
//
// NOTE ON PANEL CONTENT:
//   Every panel reads SessionManager on its own initialize() — it decides
//   internally which buttons and data to show based on permissions.
//   PostLoginRouter only decides WHICH panel to land on, not what's in it.
// ─────────────────────────────────────────────────────────────────────────────
public class PostLoginRouter {

    /**
     * Navigates to the dashboard and loads the correct default panel
     * based on the currently active session's roles.
     *
     * Call this immediately after SessionManager.initSession() completes.
     * The session MUST be initialised before calling this — we read from it.
     *
     * @param contentArea the AnchorPane in dashboard.fxml that receives panels.
     *                    Obtained via navigateToWithController after loading the dashboard.
     * 
     */
    public static void route(javafx.scene.layout.AnchorPane contentArea) {

        // The singleton session (based of Bill Plugh Model)
        SessionManager session = SessionManager.getInstance();

        // Navigate to the dashboard shell first (sidebar + content area).
        // We need the dashboard on screen before we can inject a panel into it.
        // navigateToWithController gives us the DashboardController so we can
        // call its setup method before the panel loads — but for routing we
        // just need the contentArea reference which the caller already has.

        // Determine the correct landing panel by role priority.
        ViewType landingPanel = resolveLandingPanel(session);

        // Inject the landing panel into the dashboard's content area.
        NavigationService.loadPanel(landingPanel, contentArea);
    }

    /**
     * Standalone route call used by AuthController.
     * Navigates to the dashboard, gets the DashboardController,
     * triggers sidebar setup, then loads the correct landing panel.
     *
     * This is the method AuthController actually calls after login.
     */
    public static void routeFromLogin() {

        // Step 1: Navigate to dashboard.fxml and retrieve its controller.
        // DashboardController.initialize() will run automatically on load —
        // it sets up the sidebar visibility based on permissions.
        DashboardController dashCtrl =
            NavigationService.navigateToWithController(ViewType.DASHBOARD);

        // Step 2: Tell the dashboard which panel to show first.
        // DashboardController exposes loadDefaultPanel() for exactly this.
        dashCtrl.loadDefaultPanel();
    }

    // ── Internal Logic ────────────────────────────────────────────────────────

    /**
     * Determines which panel a user should land on based on their roles.
     *
     * Priority order matters — a user who is both Staff and Customer
     * (which shouldn't happen per the schema, but defensively handled)
     * gets the Staff landing, not the Customer one.
     */
    public static ViewType resolveLandingPanel(SessionManager session) {

        // Admin gets home — they have everything, home stats are most useful.
        if (session.hasRole("Admin")) {
            return ViewType.PANEL_HOME;
        }

        // Any Staff role (RideOperator, FinanceManager, etc.) lands on home.
        // Their specific nav buttons are already filtered by permissions in
        // DashboardController — home shows them relevant stats for their roles.
        if (session.hasRole("Staff")         ||
            session.hasRole("RideOperator")  ||
            session.hasRole("RideManager")   ||
            session.hasRole("FinanceManager")||
            session.hasRole("CinemaOperator")||
            session.hasRole("BowlingOperator")||
            session.hasRole("CardOperator")  ||
            session.hasRole("StaffManager")  ||
            session.hasRole("VendorManager")) {
            return ViewType.PANEL_HOME;
        }

        // Vendor lands on their stall — that's the first thing they care about.
        if (session.hasRole("Vendor")) {
            return ViewType.PANEL_MY_STALL;
        }

        // Customer — rides is the most common first action.
        // Falls through here for any authenticated user with no special role.
        return ViewType.PANEL_RIDES;
    }
}
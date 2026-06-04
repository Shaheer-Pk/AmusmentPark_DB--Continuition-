package com.amusementpark.navigation;

// ─────────────────────────────────────────────────────────────────────────────
// ViewType  –  single source of truth for every FXML path and window title.
//
// TWO CATEGORIES:
//
//   FULL_SCREEN  — replaces the entire stage scene.
//                  Used for: LOGIN, SIGNUP, DASHBOARD.
//
//   PANEL        — injected into the dashboard's content area.
//                  Used for every module view.
//                  Never triggers a full scene swap.
//
// ADDING A NEW SCREEN:
//   1. Add an entry here with the correct path and title.
//   2. Create the FXML at that path.
//   3. Nothing else changes.
//
// THIS ENUM CONTAINS ZERO ACCESS CONTROL LOGIC.
// Whether a panel gets loaded is decided by DashboardController
// reading SessionManager — not by anything in this file.
// ─────────────────────────────────────────────────────────────────────────────
public enum ViewType {

    // ── Full-screen views (whole scene replacement) ───────────────────────────
    LOGIN       ("/fxml/auth/login.fxml",     "Amusement Park — Login"),
    SIGNUP      ("/fxml/auth/signup.fxml",    "Amusement Park — Create Account"),
    DASHBOARD   ("/fxml/dashboard.fxml",      "Amusement Park — Dashboard"),

    // ── Dashboard panels (injected into content area) ─────────────────────────

    // Home — default landing panel after login
    PANEL_HOME          ("/fxml/panels/home_panel.fxml",           "Home"),

    // Rides
    PANEL_RIDES         ("/fxml/panels/rides_panel.fxml",          "Rides"),

    // Cinema
    PANEL_CINEMA        ("/fxml/panels/cinema_panel.fxml",         "Cinema"),

    // Bowling
    PANEL_BOWLING       ("/fxml/panels/bowling_panel.fxml",        "Bowling"),

    // Card — customer self-service (view balance, recharge)
    PANEL_CARD          ("/fxml/panels/card_panel.fxml",           "My Card"),

    // Card Management — staff who manage card statuses and loyalty points
    PANEL_CARD_MGMT     ("/fxml/panels/card_management_panel.fxml","Card Management"),

    // Finance
    PANEL_FINANCE       ("/fxml/panels/finance_panel.fxml",        "Finance"),

    // Staff Management
    PANEL_STAFF         ("/fxml/panels/staff_panel.fxml",          "Staff Management"),

    // Vendor Management — staff-side contract management
    PANEL_VENDORS       ("/fxml/panels/vendor_panel.fxml",         "Vendor Management"),

    // My Stall — vendor's own stall self-management
    PANEL_MY_STALL      ("/fxml/panels/my_stall_panel.fxml",       "My Stall"),

    // Reports & Analytics
    PANEL_REPORTS       ("/fxml/panels/reports_panel.fxml",        "Reports & Analytics"),

    // System — admin-only roles/permissions management
    PANEL_SYSTEM        ("/fxml/panels/system_panel.fxml",         "System Settings"),

    // Profile — every logged-in user
    PANEL_PROFILE       ("/fxml/panels/profile_panel.fxml",        "My Profile");

    // ─────────────────────────────────────────────────────────────────────────

    private final String fxmlPath;
    private final String displayTitle;

    ViewType(String fxmlPath, String displayTitle) {
        this.fxmlPath     = fxmlPath;
        this.displayTitle = displayTitle;
    }

    public String getFxmlPath()    { return fxmlPath;     }
    public String getDisplayTitle(){ return displayTitle; }

    /** Convenience: full window title. Used by NavigationService on scene swap. */
    public String getWindowTitle() {
        // Full-screen views get the display title directly.
        // Panels don't set the window title — the dashboard chrome does.
        return displayTitle;
    }
}
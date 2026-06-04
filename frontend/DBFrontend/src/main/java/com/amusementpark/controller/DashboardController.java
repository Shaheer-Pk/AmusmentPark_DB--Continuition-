package com.amusementpark.controller;

import com.amusementpark.navigation.NavigationService;
import com.amusementpark.navigation.PostLoginRouter;
import com.amusementpark.navigation.ViewType;
import com.amusementpark.session.SessionManager;
import com.amusementpark.util.AlertHelper;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

// ─────────────────────────────────────────────────────────────────────────────
// DashboardController — controls the permanent dashboard shell.
//
// WHAT THIS CONTROLLER OWNS:
//   - The header bar (user name + logout button)
//   - The sidebar nav buttons (shown/hidden based on session permissions)
//   - The content AnchorPane (receives injected panels)
//
// WHAT THIS CONTROLLER DOES NOT DO:
//   - No database calls. Zero. Everything comes from SessionManager.
//   - No business logic. It routes and draws — nothing else.
//
// LIFECYCLE:
//   1. PostLoginRouter calls NavigationService.navigateToWithController(DASHBOARD)
//   2. JavaFX loads dashboard.fxml, creates this controller, injects @FXML fields
//   3. initialize() runs automatically — reads SessionManager, hides/shows buttons
//   4. PostLoginRouter then calls loadDefaultPanel() — injects the landing panel
//   5. User clicks nav buttons — each calls NavigationService.loadPanel()
//   6. User clicks Logout — session cleared, back to login.fxml
//
// PERMISSION MAPPING:
//   Each nav button is visible if the user holds ANY of its listed permissions.
//   If a user has zero relevant permissions for a module, that button is never
//   rendered — they cannot access what they cannot see.
// ─────────────────────────────────────────────────────────────────────────────
public class DashboardController {

    // ── Header ────────────────────────────────────────────────────────────────

    // Displays the logged-in user's full name — sourced from SessionManager,
    // no DB call needed.
    @FXML private Label  userNameLabel;

    // ── Content Area ──────────────────────────────────────────────────────────

    // The centre AnchorPane that receives all injected panels.
    // NavigationService.loadPanel() clears this and injects the new panel.
    // The sidebar and header are in separate regions and are never touched.
    @FXML private AnchorPane contentArea;

    // ── Sidebar Nav Buttons ───────────────────────────────────────────────────
    // Every button below maps to one or more permissions.
    // initialize() sets each button's visibility once and never again.
    // These buttons are called from the fxml file through fx:id

    @FXML private Button navHome;
    @FXML private Button navRides;
    @FXML private Button navCinema;
    @FXML private Button navBowling;
    @FXML private Button navMyCard;
    @FXML private Button navCardMgmt;
    @FXML private Button navFinance;
    @FXML private Button navStaff;
    @FXML private Button navVendors;
    @FXML private Button navMyStall;
    @FXML private Button navReports;
    @FXML private Button navSystem;
    @FXML private Button navProfile;

    // ── Session Reference ─────────────────────────────────────────────────────

    // Stored once at initialize() — used throughout for permission checks.
    // Never re-fetched because the session never changes while the dashboard
    // is alive. If it could change, we'd re-fetch. It can't — logout destroys
    // this entire controller and creates a fresh one on next login.
    private SessionManager session;

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Called automatically by JavaFX after all @FXML fields are injected.
     * Runs before the dashboard is visible on screen.
     *
     * Two jobs:
     *   1. Populate the header with the user's name from SessionManager.
     *   2. Show or hide every nav button based on the user's permissions.
     */
    @FXML
    private void initialize() {
        session = SessionManager.getInstance();

        // ── Header: user's full name ──────────────────────────────────────────
        // getCurrentUser() is safe here — session was initialised by AuthController
        // before PostLoginRouter navigated to this screen.
        userNameLabel.setText(session.getCurrentUser().getFullName());

        // ── Sidebar: permission-driven visibility ─────────────────────────────
        // Home is always visible — every authenticated user gets a home panel.
        // All other buttons are conditional on the permission checks below.
        navHome.setVisible(true);
        navHome.setManaged(true);

        // Rides — customers browse and purchase, operators manage status,
        // managers create/edit/delete.
        setNavVisible(navRides,
            "VIEW_RIDES", "PURCHASE_RIDE",
            "VIEW_ASSIGNED_RIDES", "UPDATE_RIDE_STATUS",
            "CREATE_RIDE", "UPDATE_RIDE", "DELETE_RIDE", "VIEW_RIDE_USAGE");

        // Cinema — customers buy tickets, operators manage screenings and halls.
        setNavVisible(navCinema,
            "VIEW_MOVIES", "PURCHASE_TICKET",
            "MANAGE_MOVIES", "MANAGE_SCREENINGS",
            "MANAGE_SEATS", "MANAGE_CINEMAHALLS");

        // Bowling — customers book lanes, operators manage sessions and lanes.
        setNavVisible(navBowling,
            "VIEW_BOWLING", "BOOK_BOWLING", "MANAGE_BOWLING");

        // My Card — customer self-service: view balance, recharge.
        setNavVisible(navMyCard,
            "VIEW_CARD", "RECHARGE_CARD");

        // Card Management — staff who blacklist cards or edit loyalty points.
        // Separate from My Card — different users, different operations.
        setNavVisible(navCardMgmt,
            "UPDATE_CARD_STATUS", "UPDATE_CARD_LOYALTYPOINTS");

        // Finance — revenue, ledger, salaries, refunds, contract revenue.
        setNavVisible(navFinance,
            "VIEW_REVENUE", "VIEW_LEDGER",
            "EDIT_SALARY", "PROCESS_REFUND", "VIEW_CONTRACT_REVENUE");

        // Staff Management — view, create, update, delete staff and assign roles.
        setNavVisible(navStaff,
            "VIEW_STAFF", "CREATE_STAFF",
            "UPDATE_STAFF", "DELETE_STAFF", "ASSIGN_ROLE");

        // Vendors — staff-side contract management and vendor revenue tracking.
        setNavVisible(navVendors,
            "VIEW_VENDOR_CONTRACT", "MANAGE_VENDOR_CONTRACT",
            "VIEW_VENDOR_REVENUE", "VIEW_STALL");

        // My Stall — vendor's own stall self-management.
        // Deliberately separate from Vendors — vendors manage their own stall,
        // not other vendors' contracts.
        setNavVisible(navMyStall,
            "MANAGE_STALL");

        // Reports & Analytics.
        setNavVisible(navReports,
            "VIEW_REPORTS", "EXPORT_REPORTS", "VIEW_ANALYTICS");

        // System — roles, permissions, audit log. Admin territory.
        setNavVisible(navSystem,
            "SYSTEM_ADMIN", "VIEW_SYSTEM_AUDIT",
            "MANAGE_ROLES", "MANAGE_PERMISSIONS");

        // Profile — every authenticated user can view and edit their own profile.
        setNavVisible(navProfile,
            "VIEW_PROFILE", "EDIT_PROFILE");
    }

    /**
     * Called by PostLoginRouter immediately after initialize() completes.
     * Loads the correct landing panel based on the user's roles.
     *
     * This is separate from initialize() because the routing decision
     * belongs to PostLoginRouter, not to this controller.
     * DashboardController just executes the injection when told to.
     */
    public void loadDefaultPanel() {
        ViewType landingPanel = PostLoginRouter.resolveLandingPanel(session);
        NavigationService.loadPanel(landingPanel, contentArea);
    }

    // ── Nav Button Handlers ───────────────────────────────────────────────────
    // Each method loads its panel into contentArea.
    // No permission check needed here — the button is only visible if the
    // user already has at least one relevant permission (set in initialize()).
    // These methods are called in the fxml file through #

    @FXML private void onNavHome()     { loadPanel(ViewType.PANEL_HOME);      }
    @FXML private void onNavRides()    { loadPanel(ViewType.PANEL_RIDES);     }
    @FXML private void onNavCinema()   { loadPanel(ViewType.PANEL_CINEMA);    }
    @FXML private void onNavBowling()  { loadPanel(ViewType.PANEL_BOWLING);   }
    @FXML private void onNavMyCard()   { loadPanel(ViewType.PANEL_CARD);      }
    @FXML private void onNavCardMgmt() { loadPanel(ViewType.PANEL_CARD_MGMT); }
    @FXML private void onNavFinance()  { loadPanel(ViewType.PANEL_FINANCE);   }
    @FXML private void onNavStaff()    { loadPanel(ViewType.PANEL_STAFF);     }
    @FXML private void onNavVendors()  { loadPanel(ViewType.PANEL_VENDORS);   }
    @FXML private void onNavMyStall()  { loadPanel(ViewType.PANEL_MY_STALL);  }
    @FXML private void onNavReports()  { loadPanel(ViewType.PANEL_REPORTS);   }
    @FXML private void onNavSystem()   { loadPanel(ViewType.PANEL_SYSTEM);    }
    @FXML private void onNavProfile()  { loadPanel(ViewType.PANEL_PROFILE);   }

    // ── Logout Handler ────────────────────────────────────────────────────────

    /**
     * Clears the session and returns to the login screen.
     *
     * Confirm before logging out — prevents accidental logouts mid-operation.
     * SessionManager.clearSession() wipes user, roles, and permissions.
     * The dashboard controller and all its panels are discarded when
     * NavigationService replaces the scene with login.fxml.
     */
    @FXML
    private void handleLogout() {
        boolean confirmed = AlertHelper.showConfirm(
            "Logout", "Are you sure you want to log out?");

        if (confirmed) {
            session.clearSession();
            NavigationService.navigateTo(ViewType.LOGIN);
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Shows or hides a nav button based on whether the current session holds
     * ANY of the listed permissions.
     *
     * setManaged(visible) is called alongside setVisible(visible).
     * WHY: setVisible(false) hides the button but it still occupies space
     * in the VBox layout — you get a blank gap where the button was.
     * setManaged(false) tells the layout engine to ignore the node entirely,
     * collapsing the space so the remaining buttons flow together cleanly.
     *
     * @param button      the sidebar nav button to show or hide
     * @param permissions one or more permission names — button shows if ANY match
     */
    private void setNavVisible(Button button, String... permissions) {
        boolean visible = session.hasAnyPermission(permissions);
        button.setVisible(visible);
        button.setManaged(visible);
    }

    /**
     * Loads a panel into the content area.
     * Extracted to avoid repeating NavigationService.loadPanel(view, contentArea)
     * in every single nav handler above.
     */
    private void loadPanel(ViewType view) {
        NavigationService.loadPanel(view, contentArea);
    }
}
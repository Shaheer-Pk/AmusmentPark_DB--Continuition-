package com.amusementpark.controller;

import com.amusementpark.dao.HomeDAO;
import com.amusementpark.session.SessionManager;
import com.amusementpark.util.AlertHelper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;

// ─────────────────────────────────────────────────────────────────────────────
// HomePanelController  —  landing panel shown after login.
//
// PERMISSION GATING:
//   Each stat card VBox and the revenue breakdown card are shown/hidden
//   independently. A customer with no staff/finance permissions sees only
//   the welcome message — all cards are hidden via setManaged(false).
//
//   revenueBreakdownCard is gated by the same VIEW_REVENUE check as
//   revenueCard — both collapse together for non-finance users.
//
// THREADING:
//   initialize() gates visibility instantly (no DB, FX thread safe).
//   loadStats() fires one background Task for all HomeDAO queries.
//   onSucceeded pushes results back to FX thread for label updates.
//   Refresh button re-fires the same loadStats() Task.
// ─────────────────────────────────────────────────────────────────────────────
public class HomePanelController {

    // ── FXML: Header ──────────────────────────────────────────────────────────
    @FXML private Label  welcomeLabel;
    @FXML private Label  statusLabel;
    @FXML private Button refreshButton;

    // ── FXML: Stat card containers ────────────────────────────────────────────
    @FXML private VBox revenueCard;
    @FXML private VBox ridesCard;
    @FXML private VBox staffCard;
    @FXML private VBox customersCard;
    @FXML private VBox stallsCard;

    // ── FXML: Revenue breakdown card (separate from stat cards row) ───────────
    // Gated by the same VIEW_REVENUE permission as revenueCard.
    // Customers never see this — setManaged(false) collapses it completely.
    @FXML private VBox revenueBreakdownCard;

    // ── FXML: Stat value labels ───────────────────────────────────────────────
    @FXML private Label totalRevenueLabel;
    @FXML private Label rideRevenueLabel;
    @FXML private Label cinemaRevenueLabel;
    @FXML private Label bowlingRevenueLabel;
    @FXML private Label rentalRevenueLabel;
    @FXML private Label activeRidesLabel;
    @FXML private Label staffCountLabel;
    @FXML private Label customerCountLabel;
    @FXML private Label stallCountLabel;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final HomeDAO        homeDAO = new HomeDAO();
    private final SessionManager session = SessionManager.getInstance();

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    private void initialize() {

        welcomeLabel.setText("Welcome back, "
            + session.getCurrentUser().getFirstName() + "!");

        // ── Gate each card by its required permission ─────────────────────────
        boolean canViewRevenue = session.hasPermission("VIEW_REVENUE");
        boolean canViewRides   = session.hasAnyPermission("VIEW_RIDES", "VIEW_ASSIGNED_RIDES");
        boolean canViewStaff   = session.hasPermission("VIEW_STAFF");
        boolean canViewStalls  = session.hasPermission("VIEW_STALL");

        setCardVisible(revenueCard,          canViewRevenue);
        setCardVisible(revenueBreakdownCard, canViewRevenue); // collapses with revenue card
        setCardVisible(ridesCard,            canViewRides);
        setCardVisible(staffCard,            canViewStaff);
        setCardVisible(customersCard,        canViewStaff);
        setCardVisible(stallsCard,           canViewStalls);

        // Refresh button only meaningful if there is something to refresh
        boolean hasAnyStats = canViewRevenue || canViewRides
                           || canViewStaff  || canViewStalls;
        refreshButton.setVisible(hasAnyStats);
        refreshButton.setManaged(hasAnyStats);

        // Load numbers if there is anything to show
        if (hasAnyStats) loadStats();
    }

    // ── Refresh Handler ───────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() { loadStats(); }

    // ── Background Data Load ──────────────────────────────────────────────────

    /**
     * Fires one background Task that runs every HomeDAO query the user
     * is permitted to see, then hands all results back to the FX thread
     * via onSucceeded to update labels atomically.
     *
     * One Task = one worker thread = one clean success/failure path.
     */
    private void loadStats() {
        statusLabel.setText("Refreshing...");

        Task<StatsResult> task = new Task<>() {
            @Override
            protected StatsResult call() throws SQLException {
                StatsResult result = new StatsResult();

                if (session.hasPermission("VIEW_REVENUE")) {
                    result.totalRevenue   = homeDAO.getTotalRevenue();
                    result.rideRevenue    = homeDAO.getRevenueByType("RIDE");
                    result.cinemaRevenue  = homeDAO.getRevenueByType("CINEMA");
                    result.bowlingRevenue = homeDAO.getRevenueByType("BOWLING");
                    result.rentalRevenue = homeDAO.getRentalRevenue();

                }
                if (session.hasAnyPermission("VIEW_RIDES", "VIEW_ASSIGNED_RIDES")) {
                    result.activeRides = homeDAO.getActiveRideCount();
                }
                if (session.hasPermission("VIEW_STAFF")) {
                    result.staffCount    = homeDAO.getTotalStaffCount();
                    result.customerCount = homeDAO.getCustomerCount();
                }
                if (session.hasPermission("VIEW_STALL")) {
                    result.stallCount = homeDAO.getActiveFoodStallCount();
                }
                return result;
            }
        };

        // All label updates happen here — guaranteed on FX thread by JavaFX Task
        task.setOnSucceeded(e -> {
            StatsResult r = task.getValue();

            if (session.hasPermission("VIEW_REVENUE")) {
                totalRevenueLabel.setText("PKR " + r.totalRevenue);
                rideRevenueLabel.setText("PKR " + r.rideRevenue);
                cinemaRevenueLabel.setText("PKR " + r.cinemaRevenue);
                bowlingRevenueLabel.setText("PKR " + r.bowlingRevenue);
                rentalRevenueLabel.setText("PKR " + r.rentalRevenue);
            }
            if (session.hasAnyPermission("VIEW_RIDES", "VIEW_ASSIGNED_RIDES")) {
                activeRidesLabel.setText(String.valueOf(r.activeRides));
            }
            if (session.hasPermission("VIEW_STAFF")) {
                staffCountLabel.setText(String.valueOf(r.staffCount));
                customerCountLabel.setText(String.valueOf(r.customerCount));
            }
            if (session.hasPermission("VIEW_STALL")) {
                stallCountLabel.setText(String.valueOf(r.stallCount));
            }

            statusLabel.setText("");
        });

        task.setOnFailed(e -> {
            statusLabel.setText("Failed to load statistics.");
            AlertHelper.showError("Load Failed",
                "Could not retrieve park statistics.");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void setCardVisible(VBox card, boolean visible) {
        card.setVisible(visible);
        card.setManaged(visible);
    }

    // ── Result Carrier ────────────────────────────────────────────────────────
    /**
     * Simple data bundle for passing Task results from worker thread to FX thread.
     * All fields default to zero — only populated for permissions the user holds.
     *
     * WHY NOT return a Map or multiple Tasks:
     *   One Task = one background thread = one clean success/failure callback.
     *   Multiple Tasks = multiple threads firing simultaneously on a shared
     *   connection pool, each needing their own callback. More complex, no benefit
     *   since home panel stats are fast scalar queries that finish in milliseconds.
     */

    private static class StatsResult {
        BigDecimal totalRevenue   = BigDecimal.ZERO;
        BigDecimal rideRevenue    = BigDecimal.ZERO;
        BigDecimal cinemaRevenue  = BigDecimal.ZERO;
        BigDecimal bowlingRevenue = BigDecimal.ZERO;
        BigDecimal rentalRevenue  = BigDecimal.ZERO;
        int        activeRides    = 0;
        int        staffCount     = 0;
        int        customerCount  = 0;
        int        stallCount     = 0;
    }
}
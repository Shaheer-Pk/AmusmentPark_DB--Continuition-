package com.amusementpark.dao;

import com.amusementpark.db.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// ─────────────────────────────────────────────────────────────────────────────
// HomeDAO  —  supplies aggregate statistics for the home panel.
//
// RETURNS SCALARS ONLY — no model class needed.
// Every method returns a single number (int or BigDecimal).
// HomePanelController calls whichever methods match the user's permissions
// and populates the corresponding stat card label.
//
// REVENUE DESIGN DECISION:
//   getTotalRevenue() counts only RECHARGE transactions.
//   When a customer recharges their card, that money enters the park system.
//   RIDE/CINEMA/BOWLING are internal movements — money already inside the system
//   moving from card to service. Not new income.
//
//   RENTAL REVENUE IS EXCLUDED HERE BY DESIGN:
//   Rent lives in Contract.ActualRent and requires date arithmetic to calculate.
//   It belongs in FinanceDAO with proper breakdown context, not on a home panel.
//
// ALL METHODS use DatabaseConnection.getActiveConn() — standalone SELECT queries.
// Each try-with-resources returns the connection to the pool automatically.
//
// THREADING:
//   Called from background Tasks in HomePanelController.
//   Never call these on the FX Application Thread directly.
// ─────────────────────────────────────────────────────────────────────────────
public class HomeDAO {

    // ── Revenue ───────────────────────────────────────────────────────────────

    /**
     * Total card-based + rental revenue earned by the park.
     * Only RECHARGE transactions — when a customer tops up their card,
     * that money enters the park system. RIDE/CINEMA/BOWLING are internal
     * movements between card balance and park services, not new income.
     *
     * Permission gate: VIEW_REVENUE
     */
    public BigDecimal getTotalRevenue() throws SQLException {
        
        // Calculates master revenue i.e all card refills + rent obtained from private food stalls
        String sql = "SELECT " +
                    " COALESCE(SUM(c.ActualRent * TIMESTAMPDIFF(MONTH, c.StartDate, c.EndDate)), 0) + " +
                    "  (SELECT COALESCE(SUM(Amount), 0) FROM CardTransaction WHERE TransactionType = 'RECHARGE') " +    // Sub-query
                    " AS total_master_revenue " + 
                    "FROM Contract c " +
                    "WHERE c.Status <> 'Pending'";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    /**
     * Revenue broken down by service type — RIDE, CINEMA, or BOWLING.
     * Shows how much money customers have spent on each service.
     * Called once per type by HomePanelController.
     *
     * Permission gate: VIEW_REVENUE
     */
    public BigDecimal getRevenueByType(String transactionType) throws SQLException {
        String sql = "SELECT COALESCE(SUM(Amount), 0) "
                   + "FROM CardTransaction "
                   + "WHERE TransactionType = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, transactionType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Total rental revenue
     * To be used in revenue breakdown portion of HomePanelController
     */
    public BigDecimal getRentalRevenue() throws SQLException {
        String sql = "SELECT " + 
                     "COALESCE(SUM(c.ActualRent * TIMESTAMPDIFF(MONTH, c.StartDate, c.EndDate)), 0)" +
                     "FROM contract c " +
                     "WHERE c.Status <> 'Pending'";
        
        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            
        }
    }

    // ── Rides ─────────────────────────────────────────────────────────────────

    /**
     * Count of rides currently marked IsOperational = TRUE.
     * Permission gate: VIEW_RIDES or VIEW_ASSIGNED_RIDES
     */
    public int getActiveRideCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ride WHERE IsOperational = TRUE";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Staff ─────────────────────────────────────────────────────────────────

    /**
     * Total number of staff records.
     * Permission gate: VIEW_STAFF
     */
    public int getTotalStaffCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Staff";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Total number of users assigned the Customer role.
     * Permission gate: VIEW_STAFF
     */
    public int getCustomerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM UserRole ur "
                   + "JOIN Role r ON ur.RoleID = r.RoleID "
                   + "WHERE r.RoleName = 'Customer'";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Vendors / Stalls ──────────────────────────────────────────────────────

    /**
     * Count of food stalls linked to an active contract.
     * Permission gate: VIEW_STALL
     */
    public int getActiveFoodStallCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM FoodStall fs "
                   + "JOIN Contract c ON fs.ContractID = c.ContractID "
                   + "WHERE c.Status = 'Active'";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
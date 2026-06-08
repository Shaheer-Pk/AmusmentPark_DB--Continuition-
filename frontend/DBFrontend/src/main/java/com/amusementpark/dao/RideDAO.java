package com.amusementpark.dao;

import com.amusementpark.db.DatabaseConnection;
import com.amusementpark.model.Ride;
import com.amusementpark.model.RideOperatorAssignment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// RideDAO  —  all database operations for the Ride module.
//
// METHODS:
//   getAllRides()          — full ride list WITHOUT operator name joined
//   getAssignedRides()     — rides assigned to a specific operator (by StaffID)
//   getRideUsageCount()    — total usage count per ride for stats
//   createRide()           — INSERT new ride (RideManager/Admin)
//   updateRide()           — UPDATE ride name and price (RideManager/Admin)
//   deleteRide()           — DELETE ride (RideManager/Admin)
//   setRideStatus()        — toggle IsOperational (RideOperator/Admin)
//   purchaseRide()         — calls stored procedure PurchaseRide(cardID, rideID)
//
// CONNECTION STRATEGY:
//   All read methods use DatabaseConnection.getActiveConn() — standalone SELECTs,
//   pooled connection returned after each call.
//   purchaseRide() also uses getActiveConn() because the transaction is handled
//   entirely inside the stored procedure on the DB side — Java just calls it.
//
// THREADING:
//   All methods called from background Tasks in RidesPanelController.
//   Never call these on the FX Application Thread.
// ─────────────────────────────────────────────────────────────────────────────
public class RideDAO {

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Fetches all rides with the necessary details to be shown to a customer.
     *
     * Used by: RideManager, Admin — full ride view (only gain management details by selecting ride).
     *          Customer — browsing available rides.
     */
    public List<Ride> getAllRides() throws SQLException {
        String sql = "SELECT r.RideID, r.RideName, r.RidePrice, r.IsOperational "    
                   + "FROM Ride r "
                   + "ORDER BY r.RideID";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return mapRides(rs);
        }
    }

    /**
     * Fetches only the rides assigned to the given operator.
     * Used by: RideOperator — sees only their own assigned rides.
     * 
     * NOTE:
     *   Now fetches the data from rideAssignmentOperator junction table
     * 
     * @param staffID the StaffID of the logged-in operator
     */
    public List<Ride> getAssignedRides(int staffID) throws SQLException {
        String sql = "SELECT r.RideID, r.RideName, r.RidePrice, "
                   + "       r.IsOperational, roa.OperatorStaffID, "
                   + "       CONCAT(u.FirstName, ' ', u.LastName) AS OperatorName "
                   + "FROM Ride r "
                   + "LEFT JOIN RideOperatorAssignment roa ON r.RideID = roa.RideID "
                   + "LEFT JOIN Staff s ON roa.StaffID = roa.StaffID "
                   + "LEFT JOIN User u ON s.UserID = u.UserID "
                   + "WHERE roa.StaffID = ? "
                   + "ORDER BY r.RideID";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRides(rs);
            }
        }
    }

    /**
     * Returns total usage count for each ride.
     * Returns list of int[]{rideID, usageCount} pairs.
     * Used by: VIEW_RIDE_USAGE permission holders.
     */
    public List<int[]> getRideUsageCounts() throws SQLException {
        String sql = "SELECT RideID, COUNT(*) AS UsageCount "
                   + "FROM RideUsage "
                   + "GROUP BY RideID "
                   + "ORDER BY RideID";

        List<int[]> results = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(new int[]{ rs.getInt("RideID"), rs.getInt("UsageCount") });
            }
        }
        return results;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new ride.
     * OperatorStaffID starts as NULL — assigned separately via setRideOperator.
     * Permission gate: CREATE_RIDE
     */
    public void createRide(String rideName, BigDecimal ridePrice) throws SQLException {
        String sql = "INSERT INTO Ride (RideName, RidePrice, IsOperational) "
                   + "VALUES (?, ?, TRUE)";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rideName);
            ps.setBigDecimal(2, ridePrice);
            ps.executeUpdate();
        }
    }

    /**
     * Updates a ride's name and price.
     * Permission gate: UPDATE_RIDE
     */
    public void updateRide(int rideID, String rideName, BigDecimal ridePrice) throws SQLException {
        String sql = "UPDATE Ride SET RideName = ?, RidePrice = ? WHERE RideID = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rideName);
            ps.setBigDecimal(2, ridePrice);
            ps.setInt(3, rideID);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a ride by ID.
     * Permission gate: DELETE_RIDE
     */
    public void deleteRide(int rideID) throws SQLException {
        String sql = "DELETE FROM Ride WHERE RideID = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rideID);
            ps.executeUpdate();
        }
    }

    /**
     * Toggles a ride's operational status.
     * Permission gate: UPDATE_RIDE_STATUS
     *
     * @param rideID      the ride to update
     * @param operational true = operational, false = offline
     */
    public void setRideStatus(int rideID, boolean operational) throws SQLException {
        String sql = "UPDATE Ride SET IsOperational = ? WHERE RideID = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, operational);
            ps.setInt(2, rideID);
            ps.executeUpdate();
        }
    }

    /**
     * Calls the PurchaseRide stored procedure.
     *
     * The procedure handles entirely on the DB side:
     *   - Balance check
     *   - Balance deduction
     *   - RideUsage insert
     *   - CardTransaction ledger insert
     *   - All inside a transaction with proper rollback on failure
     *
     * Java does NOT manually deduct balance or insert ledger entries.
     * The stored procedure is the source of truth per the architecture notes.
     *
     * @param cardID the customer's card ID
     * @param rideID the ride being purchased
     * @throws SQLException with message "Insufficient balance" or "Ride unavailable"
     *                      if the procedure signals an error
     */
    public void purchaseRide(int cardID, int rideID) throws SQLException {
        String sql = "{CALL PurchaseRide(?, ?)}";

        try (Connection conn = DatabaseConnection.getActiveConn();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, cardID);
            cs.setInt(2, rideID);
            cs.execute();
        }
    }

    // ── RideManager Based Manipulations ───────────────────────────────────────────────────────

    /**
     * WHAT IT DOES: 
     *   Fetches all rideOperators for a specific ride
     *   and validates if that rideOperator reports to 
     *   the manager fetching the operator list
     *   aka if he is a sub-oridnate of the manager or not
     * 
     * WHAT IT DOES NOT DO:
     *   It does not find RideOperators (aka staff) who
     *   AREN'T assigned to any rides. That is the job for 
     *   getAssignableOperators(int rideID, int managerStaffID)
     * 
     * @param RideID            Selected ride id (in controller's tableView)
     * @param managerStaffID    ManagerID (retrieved by session) - validates 'reports to' of operators to the session Manager staff ID    
     * @return                  List of rideOperators to the selected ride
     * @throws SQLException
     */
    public List<RideOperatorAssignment> getOperatorsForRide (int rideID, int managerStaffID) throws SQLException {
        String sql = "SELECT s.StaffID, "
                   + "  CONCAT(u.FirstName,' ',u.LastName) AS OperatorName, "
                   + "  COUNT(roa2.RideID) AS TotalAssignedRides, "
                   + "  CASE WHEN s.ReportsTo = ? THEN TRUE ELSE FALSE "          // Check if the RideOperator reports to given manager and store in 'Removeable' (See RideOperatorAssignment.java for more info)
                   + "  END AS Removeable "
                   + "FROM RideOperatorAssignment roa "
                   + "JOIN Staff s ON roa.StaffID = s.StaffID "     // So we obtain only those staff members who are ASSIGNED TO a ride
                   + "JOIN User u ON u.UserID = s.UserID "
                   + "LEFT JOIN RideOperatorAssignment roa2 ON s.StaffID = roa2.StaffID "   // So that we can fetch how many TOTAL rides the rideOperator operates on and NOT JUST STOP COUNTING AT THE SPECIFIC RIDE
                   + "WHERE roa.RideID = ? "                    // This limits the rideOperator records by limiting it to OPERATORS ASSIGNED TO THE SPECIFIC RIDE
                   + "GROUP BY s.StaffID, u.FirstName, u.LastName, s.ReportsTo";
        
        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
                
            ps.setInt(1, managerStaffID);       // for the CASE WHEN s.ReportsTo = ?
            ps.setInt(2, rideID);               // for the WHERE roa.RideID = ?

            try (ResultSet rs = ps.executeQuery()) {
                return mapRideOperators(rs);
            }
        }
    }

    /**
     * WHAT IT DOES: 
     *   Fetches all rideOperators and validates if 
     *   that rideOperator reports to the manager 
     *   fetching the operator list.
     *   Aka if he is a sub-oridnate of the manager or not.
     *   Moreover it also checks that the operator is not
     *   assigned to the SELECTED RIDE
     * 
     * WHAT IT DOES NOT DO:
     *   It does not find RideOperators (aka staff) who
     *   ARE assigned to the specifc selected ride. That is the job for 
     *   getOperatorsForRide(int rideID, int managerStaffID)
     * 
     * @param RideID            Selected ride id (in controller's tableView)
     * @param managerStaffID    ManagerID (retrieved by session) - validates 'reports to' of operators to the session Manager staff ID    
     * @return                  List of rideOperators that answers to the RideManager
     */
    public List<RideOperatorAssignment> getAssignableOperators (int rideID, int managerStaffID) throws SQLException {
        String sql = "SELECT s.StaffID, "
                   + "  CONCAT(u.FirstName, ' ', u.LastName) AS OperatorName, "
                   + "  COUNT(roa.RideID) AS TotalAssignedRides, "
                   + "  TRUE AS Removeable "                    // We want rideOperators tied to this manager
                   + "FROM Staff s "
                   + "JOIN User u ON u.UserID = s.UserID "
                   + "JOIN UserRole ur ON ur.UserID = u.UserID "
                   + "JOIN Role r ON r.RoleID = ur.RoleID "
                   + "LEFT JOIN RideOperatorAssignment roa ON s.StaffID = roa.StaffID "
                   + "WHERE r.RoleName = 'RideOperator' "
                   + "AND s.ReportsTo = ? "
                   + "AND s.StaffID NOT IN ( "
                   + "  SELECT StaffID FROM RideOperatorAssignment WHERE RideID = ? "   // This gaurantees that we get RideOperators who haven't been assigned to THIS SPECIFIC RIDE
                   + ") "
                   + "GROUP BY s.StaffID, u.FirstName, u.LastName";
        
        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, managerStaffID);
            ps.setInt(2, rideID);
            
            try (ResultSet rs = ps.executeQuery()) {
                return mapRideOperators(rs);
            }

        }
    }

    /**
     * 
     * @param rs
     * @return
     * @throws SQLException
     */
    public void assignOperatorToRide (int StaffID, int RideID) throws SQLException{
        String sql = "INSERT IGNORE INTO RideOperatorAssignment VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, StaffID);
            ps.setInt(2, RideID);

            ps.executeUpdate();
        }
    }

    /**
     * 
     * @param StaffID
     * @param RideID
     * @throws SQLException
     */
    public void removeOperatorFromRide (int StaffID, int RideID) throws SQLException {
        String sql = "DELETE FROM RideOperatorAssignment WHERE StaffID = ? AND RideID = ?";

        try (Connection conn = DatabaseConnection.getActiveConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, StaffID);
            ps.setInt(2, RideID);

            ps.executeUpdate();
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a ResultSet to a list of Ride objects.
     * Extracted to avoid duplicating the same column-mapping code in
     * getAllRides() and getAssignedRides().
     *
     * Notes:
     *   OperatorStaffID was old design, now shifted to a junction table approach and,
     *   made a seperate Model file for it
     */
    private List<Ride> mapRides(ResultSet rs) throws SQLException {
        List<Ride> rides = new ArrayList<>();
        while (rs.next()) {

            rides.add(new Ride(
                rs.getInt("RideID"),
                rs.getString("RideName"),
                rs.getBigDecimal("RidePrice"),
                rs.getBoolean("IsOperational")
            ));
        }
        return rides;
    }

    /**
     * Maps a ResultSet to a list of RideOperatorAssignment objects
     * Extracted to avoid duplicating the same column-mapping code in
     * getOperatorsForRide(int rideID, int managerStaffID) and 
     * getAssignableOperators(int rideID, int managerStaffID)
     * 
     * NOTE:
     *   This was the new design shift mentioned in above mapRides
     */
    private List<RideOperatorAssignment> mapRideOperators (ResultSet rs) throws SQLException {
        List<RideOperatorAssignment> rideOperatorAssignments = new ArrayList<>();
        while (rs.next()) {
            rideOperatorAssignments.add(new RideOperatorAssignment(
                rs.getInt("StaffID"),
                rs.getString("OperatorName"),
                rs.getInt("TotalAssignedRides"),
                rs.getBoolean("Removeable")
            ));
        }
        return rideOperatorAssignments;
    }
}
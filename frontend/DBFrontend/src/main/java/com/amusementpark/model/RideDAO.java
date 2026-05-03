package com.amusementpark.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RideDAO extends BaseDAO<Ride> {

    public List<Ride> findAll() throws SQLException {
        return executeQuery(
            "SELECT r.*, CONCAT(s.First_Name,' ',s.Last_Name) AS op_name " +
            "FROM Ride r LEFT JOIN Staff s ON r.OperatorID = s.StaffID " +
            "ORDER BY r.Ride_Name"
        );
    }

    public List<Ride> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeQuery(
            "SELECT r.*, CONCAT(s.First_Name,' ',s.Last_Name) AS op_name " +
            "FROM Ride r LEFT JOIN Staff s ON r.OperatorID = s.StaffID " +
            "WHERE r.Ride_Name LIKE ? ORDER BY r.Ride_Name", like
        );
    }

    public int insert(Ride r) throws SQLException {
        return executeUpdate(
            "INSERT INTO Ride (Ride_Name, Status, OperatorID) VALUES (?, ?, ?)",
            r.getRideName(), r.isStatus(), r.getOperatorId()
        );
    }

    public void update(Ride r) throws SQLException {
        executeUpdate(
            "UPDATE Ride SET Ride_Name=?, Status=?, OperatorID=? WHERE RideID=?",
            r.getRideName(), r.isStatus(), r.getOperatorId(), r.getRideId()
        );
    }

    /** Ride has no FK children — safe to delete directly. */
    public void delete(int rideId) throws SQLException {
        executeUpdate("DELETE FROM Ride WHERE RideID=?", rideId);
    }

    @Override
    protected Ride mapRow(ResultSet rs) throws SQLException {
        int opIdRaw = rs.getInt("OperatorID");
        Integer opId = rs.wasNull() ? null : opIdRaw;
        String opName = rs.getString("op_name");
        return new Ride(
            rs.getInt("RideID"),
            rs.getString("Ride_Name"),
            rs.getBoolean("Status"),
            opId,
            opName != null ? opName : "—"
        );
    }
}

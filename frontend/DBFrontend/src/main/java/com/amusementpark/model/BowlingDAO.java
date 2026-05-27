package com.amusementpark.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class BowlingDAO extends BaseDAO<BowlingBooking> {

    public List<BowlingBooking> findAll() throws SQLException {
        return executeQuery("SELECT * FROM Bowling_Booking ORDER BY Time DESC");
    }

    public List<BowlingBooking> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeQuery(
            "SELECT * FROM Bowling_Booking WHERE CAST(Lane_Number AS CHAR) LIKE ? ORDER BY Time DESC",
            like
        );
    }

    public int insert(BowlingBooking b) throws SQLException {
        return executeUpdate(
            "INSERT INTO Bowling_Booking (Lane_Number, Time, Amount, CardID) VALUES (?,?,?,?)",
            b.getLaneNumber(), b.getTime(), b.getAmount(), b.getCardId()
        );
    }

    public void update(BowlingBooking b) throws SQLException {
        executeUpdate(
            "UPDATE Bowling_Booking SET Lane_Number=?, Time=?, Amount=?, CardID=? WHERE BookingID=?",
            b.getLaneNumber(), b.getTime(), b.getAmount(), b.getCardId(), b.getBookingId()
        );
    }

    public void delete(int bookingId) throws SQLException {
        executeUpdate("DELETE FROM Bowling_Booking WHERE BookingID=?", bookingId);
    }

    @Override
    protected BowlingBooking mapRow(ResultSet rs) throws SQLException {
        return new BowlingBooking(
            rs.getInt("BookingID"),
            rs.getInt("Lane_Number"),
            rs.getTimestamp("Time") != null ? rs.getTimestamp("Time").toLocalDateTime() : null,
            rs.getBigDecimal("Amount"),
            rs.getInt("CardID")
        );
    }
}

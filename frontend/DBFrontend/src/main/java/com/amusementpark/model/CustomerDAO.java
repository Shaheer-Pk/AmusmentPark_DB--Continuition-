package com.amusementpark.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CustomerDAO extends BaseDAO<CustomerCard> {

    private static final String JOIN_SQL =
        "SELECT c.*, ca.CardID, ca.Balance, ca.Points " +
        "FROM Customer c LEFT JOIN Card ca ON c.CustomerID = ca.CustomerID ";

    public List<CustomerCard> findAll() throws SQLException {
        return executeQuery(JOIN_SQL + "ORDER BY c.Last_Name, c.First_Name");
    }

    public List<CustomerCard> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeQuery(
            JOIN_SQL + "WHERE c.First_Name LIKE ? OR c.Last_Name LIKE ? " +
            "ORDER BY c.Last_Name, c.First_Name",
            like, like
        );
    }

    /**
     * Inserts Customer + Card in one transaction.
     * Card.CustomerID FK is ON DELETE CASCADE so deleting a customer auto-removes the card.
     */
    public void insertWithCard(CustomerCard cc) throws SQLException {
        var conn = getConnection();
        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            int custId = executeUpdate(
                "INSERT INTO Customer (First_Name, Last_Name, Type, Date_of_Birth) VALUES (?,?,?,?)",
                cc.getFirstName(), cc.getLastName(), cc.getType(),
                cc.getDob() != null ? cc.getDob().toString() : null
            );
            executeUpdate(
                "INSERT INTO Card (Balance, Points, CustomerID) VALUES (?,?,?)",
                cc.getBalance()  != null ? cc.getBalance()  : BigDecimal.ZERO,
                cc.getPoints()   != null ? cc.getPoints()   : 0,
                custId
            );
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    public void updateCustomer(CustomerCard cc) throws SQLException {
        executeUpdate(
            "UPDATE Customer SET First_Name=?, Last_Name=?, Type=?, Date_of_Birth=? WHERE CustomerID=?",
            cc.getFirstName(), cc.getLastName(), cc.getType(),
            cc.getDob() != null ? cc.getDob().toString() : null,
            cc.getCustomerId()
        );
    }

    /** Adds topUp to Balance and addPoints to Points on the linked Card row. */
    public void rechargeCard(int cardId, BigDecimal topUp, int addPoints) throws SQLException {
        executeUpdate(
            "UPDATE Card SET Balance = Balance + ?, Points = Points + ? WHERE CardID=?",
            topUp, addPoints, cardId
        );
    }

    /**
     * Guard: Card_Payment / Ticketing / Bowling_Booking all have
     * ON DELETE RESTRICT on CardID — check before deleting the customer.
     */
    public void delete(int customerId) throws SQLException {
        Object count = executeScalar(
            "SELECT COUNT(*) FROM Card ca " +
            "JOIN Card_Payment cp ON ca.CardID = cp.CardID " +
            "WHERE ca.CustomerID = ?",
            Object.class, customerId
        );
        if (count != null && ((Number) count).intValue() > 0) {
            throw new IllegalStateException(
                "Cannot delete: this customer has ride payment history. Archive instead."
            );
        }
        executeUpdate("DELETE FROM Customer WHERE CustomerID=?", customerId);
    }

    @Override
    protected CustomerCard mapRow(ResultSet rs) throws SQLException {
        int cardIdRaw = rs.getInt("CardID");
        Integer cardId = rs.wasNull() ? null : cardIdRaw;
        return new CustomerCard(
            rs.getInt("CustomerID"),
            rs.getString("First_Name"),
            rs.getString("Last_Name"),
            rs.getString("Type"),
            rs.getDate("Date_of_Birth") != null ? rs.getDate("Date_of_Birth").toLocalDate() : null,
            cardId,
            rs.getBigDecimal("Balance"),
            rs.getObject("Points", Integer.class)
        );
    }
}

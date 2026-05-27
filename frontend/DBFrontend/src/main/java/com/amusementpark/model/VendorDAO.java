package com.amusementpark.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VendorDAO extends BaseDAO<FoodOwner> {

    // ── Owners ────────────────────────────────────────────────────────────

    public List<FoodOwner> findAllOwners() throws SQLException {
        return executeQuery("SELECT * FROM Food_Owner ORDER BY Last_Name, First_Name");
    }

    public List<FoodOwner> searchOwners(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeQuery(
            "SELECT * FROM Food_Owner WHERE First_Name LIKE ? OR Last_Name LIKE ? OR Email LIKE ? " +
            "ORDER BY Last_Name, First_Name",
            like, like, like
        );
    }

    public int insertOwner(FoodOwner o) throws SQLException {
        return executeUpdate(
            "INSERT INTO Food_Owner (First_Name, Last_Name, Email, Phone) VALUES (?,?,?,?)",
            o.getFirstName(), o.getLastName(), o.getEmail(), o.getPhone()
        );
    }

    public void updateOwner(FoodOwner o) throws SQLException {
        executeUpdate(
            "UPDATE Food_Owner SET First_Name=?, Last_Name=?, Email=?, Phone=? WHERE Food_OwnerID=?",
            o.getFirstName(), o.getLastName(), o.getEmail(), o.getPhone(), o.getOwnerId()
        );
    }

    /** ON DELETE RESTRICT — guard: owner must have no stalls first */
    public void deleteOwner(int ownerId) throws SQLException {
        Object count = executeScalar(
            "SELECT COUNT(*) FROM Food_Stalls WHERE Food_OwnerID=?", Object.class, ownerId
        );
        if (count != null && ((Number) count).intValue() > 0) {
            throw new IllegalStateException(
                "Cannot delete: this owner still has active food stalls. Remove their stalls first."
            );
        }
        executeUpdate("DELETE FROM Food_Owner WHERE Food_OwnerID=?", ownerId);
    }

    // ── Stalls ────────────────────────────────────────────────────────────

    public List<FoodStall> findAllStalls() throws SQLException {
        return executeStallQuery(
            "SELECT fs.*, CONCAT(fo.First_Name,' ',fo.Last_Name) AS owner_name " +
            "FROM Food_Stalls fs " +
            "JOIN Food_Owner fo ON fs.Food_OwnerID = fo.Food_OwnerID " +
            "ORDER BY fs.Name"
        );
    }

    public List<FoodStall> searchStalls(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeStallQuery(
            "SELECT fs.*, CONCAT(fo.First_Name,' ',fo.Last_Name) AS owner_name " +
            "FROM Food_Stalls fs " +
            "JOIN Food_Owner fo ON fs.Food_OwnerID = fo.Food_OwnerID " +
            "WHERE fs.Name LIKE ? OR fs.Type LIKE ? ORDER BY fs.Name",
            like, like
        );
    }

    public int insertStall(FoodStall s) throws SQLException {
        return executeUpdate(
            "INSERT INTO Food_Stalls (Name, Rent, Type, Establish_Date, Opening_Time, Closing_Time, Food_OwnerID) " +
            "VALUES (?,?,?,?,?,?,?)",
            s.getName(), s.getRent(), s.getType(),
            s.getEstablishDate()  != null ? s.getEstablishDate().toString()  : null,
            s.getOpeningTime()    != null ? s.getOpeningTime().toString()    : null,
            s.getClosingTime()    != null ? s.getClosingTime().toString()    : null,
            s.getOwnerId()
        );
    }

    public void updateStall(FoodStall s) throws SQLException {
        executeUpdate(
            "UPDATE Food_Stalls SET Name=?, Rent=?, Type=?, Establish_Date=?, " +
            "Opening_Time=?, Closing_Time=?, Food_OwnerID=? WHERE Food_StallID=?",
            s.getName(), s.getRent(), s.getType(),
            s.getEstablishDate()  != null ? s.getEstablishDate().toString()  : null,
            s.getOpeningTime()    != null ? s.getOpeningTime().toString()    : null,
            s.getClosingTime()    != null ? s.getClosingTime().toString()    : null,
            s.getOwnerId(), s.getStallId()
        );
    }

    /** ON DELETE RESTRICT — guard: stall must have no payment records */
    public void deleteStall(int stallId) throws SQLException {
        Object count = executeScalar(
            "SELECT COUNT(*) FROM Food_Payment WHERE Food_StallID=?", Object.class, stallId
        );
        if (count != null && ((Number) count).intValue() > 0) {
            throw new IllegalStateException(
                "Cannot delete: this stall has payment records. Remove payments first."
            );
        }
        executeUpdate("DELETE FROM Food_Stalls WHERE Food_StallID=?", stallId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<FoodStall> executeStallQuery(String sql, Object... params) throws SQLException {
        List<FoodStall> list = new ArrayList<>();
        try (var ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapStallRow(rs));
            }
        }
        return list;
    }

    private FoodStall mapStallRow(ResultSet rs) throws SQLException {
        return new FoodStall(
            rs.getInt("Food_StallID"),
            rs.getString("Name"),
            rs.getBigDecimal("Rent"),
            rs.getString("Type"),
            rs.getDate("Establish_Date")  != null ? rs.getDate("Establish_Date").toLocalDate()  : null,
            rs.getTime("Opening_Time")    != null ? rs.getTime("Opening_Time").toLocalTime()    : null,
            rs.getTime("Closing_Time")    != null ? rs.getTime("Closing_Time").toLocalTime()    : null,
            rs.getInt("Food_OwnerID"),
            rs.getString("owner_name")
        );
    }

    @Override
    protected FoodOwner mapRow(ResultSet rs) throws SQLException {
        return new FoodOwner(
            rs.getInt("Food_OwnerID"),
            rs.getString("First_Name"),
            rs.getString("Last_Name"),
            rs.getString("Email"),
            rs.getString("Phone")
        );
    }
}

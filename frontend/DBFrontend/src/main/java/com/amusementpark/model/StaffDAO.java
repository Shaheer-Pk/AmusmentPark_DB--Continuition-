package com.amusementpark.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * StaffDAO: full CRUD for the Staff table.
 *
 * FK constraints in schema:
 *   - Staff.Reports_to → Staff.StaffID  (ON DELETE SET NULL)  — safe, handled by DB
 *   - Ride.OperatorID  → Staff.StaffID  (ON DELETE RESTRICT)  — MUST check before delete
 *   - Job_Post.StaffID → Staff.StaffID  (ON DELETE CASCADE)   — auto-handled by DB
 */
public class StaffDAO extends BaseDAO<Staff> {

    // ── READ ────────────────────────────────────────────────────────────────

    public List<Staff> findAll() throws SQLException {
        return executeQuery(
            "SELECT * FROM Staff ORDER BY Last_Name, First_Name"
        );
    }

    public Staff findById(int staffId) throws SQLException {
        List<Staff> r = executeQuery("SELECT * FROM Staff WHERE StaffID = ?", staffId);
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * Search by name or title — used by the TableView search bar.
     */
    public List<Staff> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeQuery(
            "SELECT * FROM Staff WHERE First_Name LIKE ? OR Last_Name LIKE ? OR Title LIKE ? " +
            "ORDER BY Last_Name, First_Name",
            like, like, like
        );
    }

    /**
     * Returns all staff eligible to be a manager for a given staffId.
     * Excludes the staff member themselves to prevent self-reporting loop.
     */
    public List<Staff> findEligibleManagers(int excludeStaffId) throws SQLException {
        return executeQuery(
            "SELECT * FROM Staff WHERE StaffID != ? ORDER BY Last_Name, First_Name",
            excludeStaffId
        );
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Insert new staff member. Returns the auto-generated StaffID.
     */
    public int insert(Staff s) throws SQLException {
        String sql = """
            INSERT INTO Staff (First_Name, Last_Name, Title, Email, Phone_Number, Salary, Reports_to)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        return executeUpdate(sql,
            s.getFirstName(),
            s.getLastName(),
            s.getTitle(),
            s.getEmail(),
            s.getPhoneNumber(),
            s.getSalary(),
            s.getReportsTo()   // may be null → BaseDAO handles setNull
        );
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public void update(Staff s) throws SQLException {
        String sql = """
            UPDATE Staff
            SET First_Name = ?, Last_Name = ?, Title = ?, Email = ?,
                Phone_Number = ?, Salary = ?, Reports_to = ?
            WHERE StaffID = ?
            """;
        executeUpdate(sql,
            s.getFirstName(),
            s.getLastName(),
            s.getTitle(),
            s.getEmail(),
            s.getPhoneNumber(),
            s.getSalary(),
            s.getReportsTo(),
            s.getStaffId()
        );
    }

    // ── DELETE (FK-SAFE) ─────────────────────────────────────────────────────

    /**
     * Checks if this staff member is currently assigned as an operator on any Ride.
     * Ride.OperatorID has ON DELETE RESTRICT — must guard this in app logic.
     */
    public boolean isAssignedToRide(int staffId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ride WHERE OperatorID = ?";
        Object count = executeScalar(sql, Object.class, staffId);
        return count != null && ((Number) count).intValue() > 0;
    }

    /**
     * Safe delete: throws a descriptive IllegalStateException if FK would block it.
     * Job_Post entries are auto-deleted (CASCADE). Reports_to references are SET NULL.
     * Only Ride assignment needs an app-level guard.
     */
    public void delete(int staffId) throws SQLException {
        if (isAssignedToRide(staffId)) {
            throw new IllegalStateException(
                "Cannot delete: this staff member is assigned as a Ride operator. " +
                "Reassign or remove the ride assignment first."
            );
        }
        executeUpdate("DELETE FROM Staff WHERE StaffID = ?", staffId);
    }

    // ── STATS (for Dashboard) ────────────────────────────────────────────────

    public int countAll() throws SQLException {
        Object count = executeScalar("SELECT COUNT(*) FROM Staff", Object.class);
        return count != null ? ((Number) count).intValue() : 0;
    }

    public BigDecimal totalSalaryBill() throws SQLException {
        Object sum = executeScalar("SELECT SUM(Salary) FROM Staff", Object.class);
        return sum != null ? new BigDecimal(sum.toString()) : BigDecimal.ZERO;
    }

    // ── ROW MAPPER ───────────────────────────────────────────────────────────

    @Override
    protected Staff mapRow(ResultSet rs) throws SQLException {
        int reportsToRaw = rs.getInt("Reports_to");
        Integer reportsTo = rs.wasNull() ? null : reportsToRaw;

        return new Staff(
            rs.getInt("StaffID"),
            rs.getString("First_Name"),
            rs.getString("Last_Name"),
            rs.getString("Title"),
            rs.getString("Email"),
            rs.getString("Phone_Number"),
            rs.getBigDecimal("Salary"),
            reportsTo
        );
    }
}

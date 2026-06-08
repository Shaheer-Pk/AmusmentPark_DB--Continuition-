package com.amusementpark.model;

// ─────────────────────────────────────────────────────────────────────────────
// RideOperatorAssignment  —  represents one operator row in the
// "View and Edit Assigned Operators" dialog.
//
// NOT a direct table mapping. This is a read-only display model built from
// a JOIN query in RideDAO.getOperatorsForRide(rideID, managerStaffID).
//
// FIELDS:
//   staffID       — the operator's StaffID (used for remove/assign DB calls)
//   operatorName  — CONCAT(FirstName, ' ', LastName) from User table
//   totalAssigned — how many rides this operator is currently assigned to
//                   (shown in the assignable list so manager knows workload)
//   removable     — true if Staff.ReportsTo == current manager's StaffID
//                   false if operator reports to a different manager
//
// USAGE:
//   Two contexts use this model:
//
//   1. Currently assigned operators on a specific ride:
//      RideDAO.getOperatorsForRide(rideID, managerStaffID)
//      removable = true  → Remove button enabled
//      removable = false → Remove button visible but disabled + tooltip
//
//   2. Assignable operators (not yet on this ride, report to this manager):
//      RideDAO.getAssignableOperators(rideID, managerStaffID)
//      totalAssigned shown as workload indicator in the list
//      removable is irrelevant here — all rows get an Assign button
//
// WHY removable LIVES HERE AND NOT IN THE CONTROLLER:
//   The controller should not be doing ReportsTo comparisons — that is
//   business logic that belongs in the query. The DAO resolves it in SQL
//   (reportsTo = managerStaffID check) and sets this flag accordingly.
//   The controller just reads it and enables/disables the button.
// ─────────────────────────────────────────────────────────────────────────────
public class RideOperatorAssignment {

    private final int     staffID;
    private final String  operatorName;
    private final int     totalAssigned;
    private final boolean removable;     // true = reports to current manager

    // ── Constructor ───────────────────────────────────────────────────────────

    public RideOperatorAssignment(int staffID, String operatorName,
                                  int totalAssigned, boolean removable) {
        this.staffID       = staffID;
        this.operatorName  = operatorName;
        this.totalAssigned = totalAssigned;
        this.removable     = removable;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getStaffID()       { return staffID;       }
    public String  getOperatorName()  { return operatorName;  }
    public int     getTotalAssigned() { return totalAssigned; }
    public boolean isRemovable()      { return removable;     }

    @Override
    public String toString() {
        return operatorName + " (" + totalAssigned + " rides)";
    }
}
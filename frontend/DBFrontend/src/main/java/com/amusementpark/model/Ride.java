package com.amusementpark.model;

import java.math.BigDecimal;

// ─────────────────────────────────────────────────────────────────────────────
// Ride  —  maps directly to the Ride table.
//
// Table columns mirrored:
//   RideID | RideName | RidePrice | IsOperational
//
// WHAT CHANGED FROM V1:
//   OperatorStaffID and OperatorName have been removed entirely.
//   Operator assignment is now a many-to-many relationship handled by the
//   RideOperatorAssignment junction table (StaffID, RideID composite PK).
//   Operator data is never loaded with rides — it is fetched on demand only
//   when a RideManager opens the "View and Edit Assigned Operators" dialog
//   for a specific ride. The Ride model is now pure ride metadata.
//
// DESIGN NOTES:
//   rideID is final — set at DB creation, never changes.
//   BigDecimal for price — DECIMAL(10,2) in MySQL must never be represented
//   as float/double (precision loss on monetary values).
//   isOperational is mutable — toggled by RideOperator/RideManager via
//   RideDAO.setRideStatus().
// ─────────────────────────────────────────────────────────────────────────────
public class Ride {

    private final int        rideID;
    private       String     rideName;
    private       BigDecimal ridePrice;
    private       boolean    isOperational;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Ride(int rideID, String rideName, BigDecimal ridePrice, boolean isOperational) {
        this.rideID        = rideID;
        this.rideName      = rideName;
        this.ridePrice     = ridePrice;
        this.isOperational = isOperational;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int        getRideID()      { return rideID;        }
    public String     getRideName()    { return rideName;      }
    public BigDecimal getRidePrice()   { return ridePrice;     }
    public boolean    isOperational()  { return isOperational; }

    // ── Setters (rideID is immutable) ─────────────────────────────────────────

    public void setRideName(String rideName)           { this.rideName      = rideName;      }
    public void setRidePrice(BigDecimal ridePrice)     { this.ridePrice     = ridePrice;     }
    public void setOperational(boolean isOperational)  { this.isOperational = isOperational; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Convenience for TableView status column — "Operational" or "Offline". */
    public String getStatusDisplay() {
        return isOperational ? "Operational" : "Offline";
    }

    @Override
    public String toString() {
        return "Ride{id=" + rideID + ", name=" + rideName + "}";
    }
}
package com.amusementpark.model;

import java.math.BigDecimal;

// ─────────────────────────────────────────────────────────────────────────────
// Card  —  maps to the Card table. Represents a customer's wallet.
//
// Table columns mirrored:
//   CardID | UserID (FK) | Balance | LoyaltyPoints | IsActive
//
// DESIGN NOTES:
//   - Only Customer accounts have a Card loaded into session (handled in AuthDAO).
//     Staff and Vendor get null from SessionManager.getCard().
//   - Balance is BigDecimal — never float/double for monetary values.
//     DECIMAL(10,2) in MySQL, BigDecimal in Java. Non-negotiable.
//   - This object is loaded once at login into SessionManager.
//     It is NOT a live view — balance shown here reflects login-time state.
//     Any purchase panel that needs current balance must re-query the DB.
//   - cardID is what gets passed to all stored procedures:
//       PurchaseRide(cardID, rideID)
//       PurchaseTicket(cardID, screeningID, seatID)
//       StartBowlingSession(cardID, laneID, durationMinutes)
//     Controllers call session.getCard().getCardID() — no per-panel DB lookup.
//
// IMMUTABILITY:
//   All fields are final. Card is loaded at login and never mutated in memory.
//   If balance changes (after purchase), the panel refreshes from DB directly.
//   We do NOT update the Card object in SessionManager mid-session — that would
//   require synchronisation and creates stale-data bugs. Read from DB when fresh
//   balance matters, use session card only for the cardID.
// ─────────────────────────────────────────────────────────────────────────────
public class Card {

    private final int        cardID;
    private final BigDecimal balance;
    private final int        loyaltyPoints;
    private final boolean    isActive;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Card(int cardID, BigDecimal balance, int loyaltyPoints, boolean isActive) {
        this.cardID        = cardID;
        this.balance       = balance;
        this.loyaltyPoints = loyaltyPoints;
        this.isActive      = isActive;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int        getCardID()        { return cardID;        }
    public BigDecimal getBalance()       { return balance;       }
    public int        getLoyaltyPoints() { return loyaltyPoints; }
    public boolean    isActive()         { return isActive;      }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Formatted balance string for display — e.g. "PKR 1,250.00" */
    public String getBalanceDisplay() {
        return "PKR " + balance.toPlainString();
    }

    @Override
    public String toString() {
        return "Card{id=" + cardID + ", balance=" + balance + "}";
    }
}
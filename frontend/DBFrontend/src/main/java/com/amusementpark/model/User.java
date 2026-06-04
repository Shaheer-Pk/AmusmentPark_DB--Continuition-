package com.amusementpark.model;

import java.time.LocalDate;

// ─────────────────────────────────────────────────────────────────────────────
// User  –  maps directly to the User table.
//
// NOTE: This is a plain data carrier (record-like).
//   No business logic lives here.
//   No auth/role/permission logic lives here.
//   That belongs in SessionManager and the DAO layer respectively.
//   We are also not using javafx.beans.property here because User isn't a UI component
//
// Table columns mirrored:
//   UserID | FirstName | LastName | PhoneNumber | DateOfBirth
// ─────────────────────────────────────────────────────────────────────────────
public class User {

    private final int       userID;
    private       String    firstName;
    private       String    lastName;
    private       String    phoneNumber;
    private       LocalDate dateOfBirth;

    // ── Constructor ───────────────────────────────────────────────────────────

    public User(int userID, String firstName, String lastName,
                String phoneNumber, LocalDate dateOfBirth) {
        this.userID      = userID;
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int       getUserID()      { return userID;      }
    public String    getFirstName()   { return firstName;   }
    public String    getLastName()    { return lastName;    }
    public String    getPhoneNumber() { return phoneNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    // ── Setters (mutable fields only — userID is immutable by design) ─────────

    public void setFirstName(String firstName)     { this.firstName   = firstName;   }
    public void setLastName(String lastName)       { this.lastName    = lastName;    }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setDateOfBirth(LocalDate dob)      { this.dateOfBirth = dob;         }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Convenience for display in headers, nav bars, etc. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "User{id=" + userID + ", name=" + getFullName() + "}";
    }
}
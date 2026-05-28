package com.amusementpark.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class UserAccount {
    private final IntegerProperty loginId = new SimpleIntegerProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final BooleanProperty isAdmin = new SimpleBooleanProperty();
    private final ObjectProperty<Integer> customerId = new SimpleObjectProperty<>();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();

    public UserAccount() {}

    public UserAccount(int loginId, String email, boolean isAdmin, Integer customerId, 
                       String firstName, String lastName, LocalDateTime createdAt) {
        setLoginId(loginId);
        setEmail(email);
        setIsAdmin(isAdmin);
        setCustomerId(customerId);
        setFirstName(firstName);
        setLastName(lastName);
        setCreatedAt(createdAt);
    }

    // Property Accessors
    public int getLoginId() { return loginId.get(); }
    public void setLoginId(int v) { loginId.set(v); }
    public IntegerProperty loginIdProperty() { return loginId; }

    public String getEmail() { return email.get(); }
    public void setEmail(String v) { email.set(v); }
    public StringProperty emailProperty() { return email; }

    public boolean isIsAdmin() { return isAdmin.get(); }
    public void setIsAdmin(boolean v) { isAdmin.set(v); }
    public BooleanProperty isAdminProperty() { return isAdmin; }

    public Integer getCustomerId() { return customerId.get(); }
    public void setCustomerId(Integer v) { customerId.set(v); }
    public ObjectProperty<Integer> customerIdProperty() { return customerId; }

    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String v) { firstName.set(v); }
    public StringProperty firstNameProperty() { return firstName; }

    public String getLastName() { return lastName.get(); }
    public void setLastName(String v) { lastName.set(v); }
    public StringProperty lastNameProperty() { return lastName; }

    // Helper helper method to return a clean concatenated display name for sidebars
    public String getFullName() {
        if (getFirstName() == null && getLastName() == null) {
            return isIsAdmin() ? "System Administrator" : "Guest User";
        }
        return getFirstName() + " " + getLastName();
    }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime v) { createdAt.set(v); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}
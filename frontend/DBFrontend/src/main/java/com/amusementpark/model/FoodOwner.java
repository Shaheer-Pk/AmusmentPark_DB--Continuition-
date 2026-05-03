package com.amusementpark.model;

import javafx.beans.property.*;

public class FoodOwner {
    private final IntegerProperty ownerId   = new SimpleIntegerProperty();
    private final StringProperty  firstName = new SimpleStringProperty();
    private final StringProperty  lastName  = new SimpleStringProperty();
    private final StringProperty  email     = new SimpleStringProperty();
    private final StringProperty  phone     = new SimpleStringProperty();

    public FoodOwner() {}

    public FoodOwner(int ownerId, String firstName, String lastName, String email, String phone) {
        setOwnerId(ownerId); setFirstName(firstName); setLastName(lastName);
        setEmail(email); setPhone(phone);
    }

    public String getFullName() { return firstName.get() + " " + lastName.get(); }

    public int getOwnerId()                      { return ownerId.get(); }
    public void setOwnerId(int v)                { ownerId.set(v); }
    public IntegerProperty ownerIdProperty()     { return ownerId; }

    public String getFirstName()                 { return firstName.get(); }
    public void setFirstName(String v)           { firstName.set(v); }
    public StringProperty firstNameProperty()    { return firstName; }

    public String getLastName()                  { return lastName.get(); }
    public void setLastName(String v)            { lastName.set(v); }
    public StringProperty lastNameProperty()     { return lastName; }

    public String getEmail()                     { return email.get(); }
    public void setEmail(String v)               { email.set(v); }
    public StringProperty emailProperty()        { return email; }

    public String getPhone()                     { return phone.get(); }
    public void setPhone(String v)               { phone.set(v); }
    public StringProperty phoneProperty()        { return phone; }

    @Override public String toString()           { return getFullName(); }
}

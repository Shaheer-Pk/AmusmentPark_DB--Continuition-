package com.amusementpark.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CustomerCard {
    private final IntegerProperty customerId  = new SimpleIntegerProperty();
    private final StringProperty  firstName   = new SimpleStringProperty();
    private final StringProperty  lastName    = new SimpleStringProperty();
    private final StringProperty  type        = new SimpleStringProperty();
    private final ObjectProperty<LocalDate>   dob     = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer>     cardId  = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal>  balance = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer>     points  = new SimpleObjectProperty<>();

    public CustomerCard() {}

    public CustomerCard(int customerId, String firstName, String lastName,
                        String type, LocalDate dob,
                        Integer cardId, BigDecimal balance, Integer points) {
        setCustomerId(customerId); setFirstName(firstName); setLastName(lastName);
        setType(type); setDob(dob); setCardId(cardId); setBalance(balance); setPoints(points);
    }

    public String getFullName() { return firstName.get() + " " + lastName.get(); }

    public int getCustomerId()                           { return customerId.get(); }
    public void setCustomerId(int v)                     { customerId.set(v); }
    public IntegerProperty customerIdProperty()          { return customerId; }

    public String getFirstName()                         { return firstName.get(); }
    public void setFirstName(String v)                   { firstName.set(v); }
    public StringProperty firstNameProperty()            { return firstName; }

    public String getLastName()                          { return lastName.get(); }
    public void setLastName(String v)                    { lastName.set(v); }
    public StringProperty lastNameProperty()             { return lastName; }

    public String getType()                              { return type.get(); }
    public void setType(String v)                        { type.set(v); }
    public StringProperty typeProperty()                 { return type; }

    public LocalDate getDob()                            { return dob.get(); }
    public void setDob(LocalDate v)                      { dob.set(v); }
    public ObjectProperty<LocalDate> dobProperty()       { return dob; }

    public Integer getCardId()                           { return cardId.get(); }
    public void setCardId(Integer v)                     { cardId.set(v); }
    public ObjectProperty<Integer> cardIdProperty()      { return cardId; }

    public BigDecimal getBalance()                       { return balance.get(); }
    public void setBalance(BigDecimal v)                 { balance.set(v); }
    public ObjectProperty<BigDecimal> balanceProperty()  { return balance; }

    public Integer getPoints()                           { return points.get(); }
    public void setPoints(Integer v)                     { points.set(v); }
    public ObjectProperty<Integer> pointsProperty()      { return points; }
}

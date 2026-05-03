package com.amusementpark.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class FoodStall {
    private final IntegerProperty stallId             = new SimpleIntegerProperty();
    private final StringProperty  name               = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> rent    = new SimpleObjectProperty<>();
    private final StringProperty  type               = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> establishDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> openingTime   = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> closingTime   = new SimpleObjectProperty<>();
    private final IntegerProperty ownerId            = new SimpleIntegerProperty();
    private final StringProperty  ownerName          = new SimpleStringProperty();

    public FoodStall() {}

    public FoodStall(int stallId, String name, BigDecimal rent, String type,
                     LocalDate establishDate, LocalTime openingTime, LocalTime closingTime,
                     int ownerId, String ownerName) {
        setStallId(stallId); setName(name); setRent(rent); setType(type);
        setEstablishDate(establishDate); setOpeningTime(openingTime); setClosingTime(closingTime);
        setOwnerId(ownerId); setOwnerName(ownerName);
    }

    public int getStallId()                                { return stallId.get(); }
    public void setStallId(int v)                          { stallId.set(v); }
    public IntegerProperty stallIdProperty()               { return stallId; }

    public String getName()                                { return name.get(); }
    public void setName(String v)                          { name.set(v); }
    public StringProperty nameProperty()                   { return name; }

    public BigDecimal getRent()                            { return rent.get(); }
    public void setRent(BigDecimal v)                      { rent.set(v); }
    public ObjectProperty<BigDecimal> rentProperty()       { return rent; }

    public String getType()                                { return type.get(); }
    public void setType(String v)                          { type.set(v); }
    public StringProperty typeProperty()                   { return type; }

    public LocalDate getEstablishDate()                    { return establishDate.get(); }
    public void setEstablishDate(LocalDate v)              { establishDate.set(v); }
    public ObjectProperty<LocalDate> establishDateProperty() { return establishDate; }

    public LocalTime getOpeningTime()                      { return openingTime.get(); }
    public void setOpeningTime(LocalTime v)                { openingTime.set(v); }
    public ObjectProperty<LocalTime> openingTimeProperty() { return openingTime; }

    public LocalTime getClosingTime()                      { return closingTime.get(); }
    public void setClosingTime(LocalTime v)                { closingTime.set(v); }
    public ObjectProperty<LocalTime> closingTimeProperty() { return closingTime; }

    public int getOwnerId()                                { return ownerId.get(); }
    public void setOwnerId(int v)                          { ownerId.set(v); }
    public IntegerProperty ownerIdProperty()               { return ownerId; }

    public String getOwnerName()                           { return ownerName.get(); }
    public void setOwnerName(String v)                     { ownerName.set(v); }
    public StringProperty ownerNameProperty()              { return ownerName; }
}

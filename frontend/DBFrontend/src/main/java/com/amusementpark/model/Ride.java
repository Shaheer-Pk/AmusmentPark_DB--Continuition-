package com.amusementpark.model;

import javafx.beans.property.*;

public class Ride {
    private final IntegerProperty rideId      = new SimpleIntegerProperty();
    private final StringProperty  rideName    = new SimpleStringProperty();
    private final BooleanProperty status      = new SimpleBooleanProperty();
    private final ObjectProperty<Integer> operatorId = new SimpleObjectProperty<>();

    // Resolved operator name — populated by join in DAO
    private final StringProperty operatorName = new SimpleStringProperty();

    public Ride() {}

    public Ride(int rideId, String rideName, boolean status, Integer operatorId, String operatorName) {
        setRideId(rideId); setRideName(rideName); setStatus(status);
        setOperatorId(operatorId); setOperatorName(operatorName);
    }

    public int getRideId()                            { return rideId.get(); }
    public void setRideId(int v)                      { rideId.set(v); }
    public IntegerProperty rideIdProperty()           { return rideId; }

    public String getRideName()                       { return rideName.get(); }
    public void setRideName(String v)                 { rideName.set(v); }
    public StringProperty rideNameProperty()          { return rideName; }

    public boolean isStatus()                         { return status.get(); }
    public void setStatus(boolean v)                  { status.set(v); }
    public BooleanProperty statusProperty()           { return status; }

    public Integer getOperatorId()                    { return operatorId.get(); }
    public void setOperatorId(Integer v)              { operatorId.set(v); }
    public ObjectProperty<Integer> operatorIdProperty() { return operatorId; }

    public String getOperatorName()                   { return operatorName.get(); }
    public void setOperatorName(String v)             { operatorName.set(v); }
    public StringProperty operatorNameProperty()      { return operatorName; }
}

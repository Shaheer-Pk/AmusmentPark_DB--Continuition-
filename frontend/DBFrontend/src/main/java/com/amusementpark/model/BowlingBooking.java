package com.amusementpark.model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BowlingBooking {
    private final IntegerProperty            bookingId  = new SimpleIntegerProperty();
    private final IntegerProperty            laneNumber = new SimpleIntegerProperty();
    private final ObjectProperty<LocalDateTime> time    = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> amount     = new SimpleObjectProperty<>();
    private final IntegerProperty            cardId     = new SimpleIntegerProperty();

    public BowlingBooking() {}

    public BowlingBooking(int bookingId, int laneNumber, LocalDateTime time,
                          BigDecimal amount, int cardId) {
        setBookingId(bookingId);
        setLaneNumber(laneNumber);
        setTime(time);
        setAmount(amount);
        setCardId(cardId);
    }

    public int getBookingId()                              { return bookingId.get(); }
    public void setBookingId(int v)                        { bookingId.set(v); }
    public IntegerProperty bookingIdProperty()             { return bookingId; }

    public int getLaneNumber()                             { return laneNumber.get(); }
    public void setLaneNumber(int v)                       { laneNumber.set(v); }
    public IntegerProperty laneNumberProperty()            { return laneNumber; }

    public LocalDateTime getTime()                         { return time.get(); }
    public void setTime(LocalDateTime v)                   { time.set(v); }
    public ObjectProperty<LocalDateTime> timeProperty()    { return time; }

    public BigDecimal getAmount()                          { return amount.get(); }
    public void setAmount(BigDecimal v)                    { amount.set(v); }
    public ObjectProperty<BigDecimal> amountProperty()     { return amount; }

    public int getCardId()                                 { return cardId.get(); }
    public void setCardId(int v)                           { cardId.set(v); }
    public IntegerProperty cardIdProperty()                { return cardId; }
}

package com.amusementpark.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Screening {
    private final IntegerProperty               screeningId   = new SimpleIntegerProperty();
    private final ObjectProperty<LocalDateTime> screeningTime = new SimpleObjectProperty<>();
    private final IntegerProperty               movieId       = new SimpleIntegerProperty();
    private final IntegerProperty               hallId        = new SimpleIntegerProperty();
    private final StringProperty                movieTitle    = new SimpleStringProperty();

    public Screening() {}

    public Screening(int screeningId, LocalDateTime screeningTime,
                     int movieId, int hallId, String movieTitle) {
        setScreeningId(screeningId);
        setScreeningTime(screeningTime);
        setMovieId(movieId);
        setHallId(hallId);
        setMovieTitle(movieTitle);
    }

    public int getScreeningId()                                  { return screeningId.get(); }
    public void setScreeningId(int v)                            { screeningId.set(v); }
    public IntegerProperty screeningIdProperty()                 { return screeningId; }

    public LocalDateTime getScreeningTime()                      { return screeningTime.get(); }
    public void setScreeningTime(LocalDateTime v)                { screeningTime.set(v); }
    public ObjectProperty<LocalDateTime> screeningTimeProperty() { return screeningTime; }

    public int getMovieId()                                      { return movieId.get(); }
    public void setMovieId(int v)                                { movieId.set(v); }
    public IntegerProperty movieIdProperty()                     { return movieId; }

    public int getHallId()                                       { return hallId.get(); }
    public void setHallId(int v)                                 { hallId.set(v); }
    public IntegerProperty hallIdProperty()                      { return hallId; }

    public String getMovieTitle()                                { return movieTitle.get(); }
    public void setMovieTitle(String v)                          { movieTitle.set(v); }
    public StringProperty movieTitleProperty()                   { return movieTitle; }
}

package com.amusementpark.model;

public class Movie {
    private int    movieId;
    private String title;
    private String rating;
    private int    duration;

    public Movie(int movieId, String title, String rating, int duration) {
        this.movieId  = movieId;
        this.title    = title;
        this.rating   = rating;
        this.duration = duration;
    }

    public int    getMovieId()   { return movieId;  }
    public String getTitle()     { return title;    }
    public String getRating()    { return rating;   }
    public int    getDuration()  { return duration; }

    @Override
    public String toString() { return title + " (" + rating + ", " + duration + " min)"; }
}

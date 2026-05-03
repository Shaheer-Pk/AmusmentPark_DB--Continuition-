package com.amusementpark.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CinemaDAO extends BaseDAO<Screening> {

    public List<Screening> findAll() throws SQLException {
        return executeQuery(
            "SELECT sc.*, m.Title AS movie_title " +
            "FROM Screening sc JOIN Movie m ON sc.MovieID = m.MovieID " +
            "ORDER BY sc.Screening_Time DESC"
        );
    }

    public List<Screening> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return executeQuery(
            "SELECT sc.*, m.Title AS movie_title " +
            "FROM Screening sc JOIN Movie m ON sc.MovieID = m.MovieID " +
            "WHERE m.Title LIKE ? ORDER BY sc.Screening_Time DESC",
            like
        );
    }

    public int insert(Screening s) throws SQLException {
        return executeUpdate(
            "INSERT INTO Screening (Screening_Time, MovieID, HallID) VALUES (?,?,?)",
            s.getScreeningTime(), s.getMovieId(), s.getHallId()
        );
    }

    public void update(Screening s) throws SQLException {
        executeUpdate(
            "UPDATE Screening SET Screening_Time=?, MovieID=?, HallID=? WHERE ScreeningID=?",
            s.getScreeningTime(), s.getMovieId(), s.getHallId(), s.getScreeningId()
        );
    }

    /** Guard: Ticketing has ON DELETE RESTRICT on ScreeningID */
    public void delete(int screeningId) throws SQLException {
        Object count = executeScalar(
            "SELECT COUNT(*) FROM Ticketing WHERE ScreeningID=?", Object.class, screeningId
        );
        if (count != null && ((Number) count).intValue() > 0) {
            throw new IllegalStateException(
                "Cannot delete: tickets have been sold for this screening."
            );
        }
        executeUpdate("DELETE FROM Screening WHERE ScreeningID=?", screeningId);
    }

    public List<Movie> findAllMovies() throws SQLException {
        List<Movie> list = new ArrayList<>();
        try (var ps = getConnection().prepareStatement("SELECT * FROM Movie ORDER BY Title");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Movie(
                    rs.getInt("MovieID"),
                    rs.getString("Title"),
                    rs.getString("Rating"),
                    rs.getInt("Duration")
                ));
            }
        }
        return list;
    }

    public List<Hall> findAllHalls() throws SQLException {
        List<Hall> list = new ArrayList<>();
        try (var ps = getConnection().prepareStatement("SELECT * FROM Cinema ORDER BY HallID");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Hall(rs.getInt("HallID"), rs.getInt("Capacity")));
            }
        }
        return list;
    }

    @Override
    protected Screening mapRow(ResultSet rs) throws SQLException {
        return new Screening(
            rs.getInt("ScreeningID"),
            rs.getTimestamp("Screening_Time") != null
                ? rs.getTimestamp("Screening_Time").toLocalDateTime() : null,
            rs.getInt("MovieID"),
            rs.getInt("HallID"),
            rs.getString("movie_title")
        );
    }
}

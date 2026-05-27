package com.amusementpark;

import com.amusementpark.model.Hall;
import com.amusementpark.model.Movie;
import com.amusementpark.model.Screening;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

public class CinemaModalController {

    @FXML private Label              modalTitle;
    @FXML private Label              modalSubtitle;
    @FXML private TextField          timeField;
    @FXML private ComboBox<Movie>    movieCombo;
    @FXML private ComboBox<Hall>     hallCombo;
    @FXML private Label              validationLabel;
    @FXML private Button             saveButton;

    private Consumer<Screening> onSave;
    private Runnable            onCancel;
    private Screening           existing;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void setMode(Screening s, List<Movie> movies, List<Hall> halls) {
        this.existing = s;
        movieCombo.getItems().setAll(movies);
        hallCombo.getItems().setAll(halls);

        if (s == null) {
            modalTitle.setText("Add Screening");
            modalSubtitle.setText("Fill in all fields below");
        } else {
            modalTitle.setText("Edit Screening");
            modalSubtitle.setText("Editing Screening #" + s.getScreeningId());
            saveButton.setText("Save Changes");
            timeField.setText(s.getScreeningTime() != null ? s.getScreeningTime().format(DT) : "");
            movies.stream().filter(m -> m.getMovieId() == s.getMovieId()).findFirst().ifPresent(movieCombo::setValue);
            halls.stream().filter(h -> h.getHallId() == s.getHallId()).findFirst().ifPresent(hallCombo::setValue);
        }
    }

    public void setOnSave(Consumer<Screening> cb)  { this.onSave   = cb; }
    public void setOnCancel(Runnable cb)            { this.onCancel = cb; }

    @FXML private void handleSave() {
        validationLabel.setText("");
        String timeStr = timeField.getText().trim();
        Movie  movie   = movieCombo.getValue();
        Hall   hall    = hallCombo.getValue();

        if (timeStr.isEmpty() || movie == null || hall == null) {
            validationLabel.setText("All fields are required."); return;
        }

        LocalDateTime time;
        try { time = LocalDateTime.parse(timeStr, DT); }
        catch (DateTimeParseException e) { validationLabel.setText("Date/Time must be YYYY-MM-DD HH:MM."); return; }

        Screening result = existing != null ? existing : new Screening();
        result.setScreeningTime(time);
        result.setMovieId(movie.getMovieId());
        result.setHallId(hall.getHallId());
        result.setMovieTitle(movie.getTitle());

        if (onSave != null) onSave.accept(result);
    }

    @FXML private void handleCancel() { if (onCancel != null) onCancel.run(); }
}

package com.amusementpark;

import com.amusementpark.model.CinemaDAO;
import com.amusementpark.model.Hall;
import com.amusementpark.model.Movie;
import com.amusementpark.model.Screening;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CinemaController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label     recordCountLabel;
    @FXML private TableView<Screening>            cinemaTable;
    @FXML private TableColumn<Screening, Integer> colId;
    @FXML private TableColumn<Screening, LocalDateTime>  colTime;
    @FXML private TableColumn<Screening, String>  colMovie;
    @FXML private TableColumn<Screening, Integer> colHall;
    @FXML private TableColumn<Screening, Void>    colActions;

    private final CinemaDAO dao = new CinemaDAO();
    private final ObservableList<Screening> masterList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override public void initialize(URL url, ResourceBundle rb) {
        bindColumns(); addActionsColumn(); loadAll();
    }

    private void bindColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("screeningId"));
        colMovie.setCellValueFactory(new PropertyValueFactory<>("movieTitle"));
        colHall.setCellValueFactory(new PropertyValueFactory<>("hallId"));

        colTime.setCellValueFactory(new PropertyValueFactory<>("screeningTime"));
        colTime.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : val.format(DT));
            }
        });

        cinemaTable.setItems(masterList);
    }

    private void addActionsColumn() {
        Callback<TableColumn<Screening, Void>, TableCell<Screening, Void>> factory = col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                deleteBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e   -> handleEdit(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
        colActions.setCellFactory(factory);
    }

    private void loadAll() {
        runAsync(dao::findAll, list -> {
            masterList.setAll(list);
            recordCountLabel.setText(list.size() + " records");
        }, "cinema-load");
    }

    @FXML private void handleSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadAll(); return; }
        runAsync(() -> dao.search(kw), list -> {
            masterList.setAll(list);
            recordCountLabel.setText(list.size() + " records");
        }, "cinema-search");
    }

    @FXML private void handleAdd() {
        openModal(null).ifPresent(s -> runAsync(() -> {
            int id = dao.insert(s); s.setScreeningId(id); return s;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Success", "Screening added."); }, "cinema-insert"));
    }

    private void handleEdit(Screening s) {
        openModal(s).ifPresent(updated -> runAsync(() -> {
            dao.update(updated); return updated;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Success", "Screening updated."); }, "cinema-update"));
    }

    private void handleDelete(Screening s) {
        if (!AlertHelper.showConfirm("Delete Screening",
                "Delete screening of '" + s.getMovieTitle() + "'?")) return;
        runAsync(() -> { dao.delete(s.getScreeningId()); return s; },
                 ignored -> { loadAll(); AlertHelper.showInfo("Deleted", "Screening removed."); }, "cinema-delete");
    }

    private Optional<Screening> openModal(Screening existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CinemaModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            CinemaModalController ctrl = loader.getController();
            List<Movie> movies; List<Hall> halls;
            try { movies = dao.findAllMovies(); halls = dao.findAllHalls(); }
            catch (SQLException e) { movies = List.of(); halls = List.of(); }
            ctrl.setMode(existing, movies, halls);

            Dialog<Screening> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(existing == null ? "Add Screening" : "Edit Screening");
            dialog.setResultConverter(b -> null);
            ctrl.setOnSave(sc -> { dialog.setResult(sc); dialog.close(); });
            ctrl.setOnCancel(dialog::close);
            return dialog.showAndWait();
        } catch (IOException ex) {
            AlertHelper.showError("Modal Error", ex.getMessage());
            return Optional.empty();
        }
    }

    @FunctionalInterface interface DbSupplier<T> { T get() throws SQLException; }
    private <T> void runAsync(DbSupplier<T> db, Consumer<T> onSuccess, String name) {
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return db.get(); } };
        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable c = task.getException();
            AlertHelper.showError("Error", c instanceof IllegalStateException ? c.getMessage() : "DB error: " + c.getMessage());
        }));
        new Thread(task, name).start();
    }
}

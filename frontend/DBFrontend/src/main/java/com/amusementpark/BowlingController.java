package com.amusementpark;

import com.amusementpark.model.BowlingBooking;
import com.amusementpark.model.BowlingDAO;

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
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class BowlingController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label     recordCountLabel;
    @FXML private TableView<BowlingBooking>            bowlingTable;
    @FXML private TableColumn<BowlingBooking, Integer> colId;
    @FXML private TableColumn<BowlingBooking, Integer> colLane;
    @FXML private TableColumn<BowlingBooking, LocalDateTime> colTime;
    @FXML private TableColumn<BowlingBooking, BigDecimal> colAmount;
    @FXML private TableColumn<BowlingBooking, Integer> colCard;
    @FXML private TableColumn<BowlingBooking, Void>    colActions;

    private final BowlingDAO dao = new BowlingDAO();
    private final ObservableList<BowlingBooking> masterList = FXCollections.observableArrayList();
    private static final NumberFormat PKR = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    static { PKR.setMinimumFractionDigits(2); PKR.setMaximumFractionDigits(2); }

    @Override public void initialize(URL url, ResourceBundle rb) {
        bindColumns(); addActionsColumn(); loadAll();
    }

    private void bindColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colLane.setCellValueFactory(new PropertyValueFactory<>("laneNumber"));
        colCard.setCellValueFactory(new PropertyValueFactory<>("cardId"));

        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTime.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : val.format(DT));
            }
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : PKR.format(val));
            }
        });

        bowlingTable.setItems(masterList);
    }

    private void addActionsColumn() {
        Callback<TableColumn<BowlingBooking, Void>, TableCell<BowlingBooking, Void>> factory = col -> new TableCell<>() {
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
        }, "bowling-load");
    }

    @FXML private void handleSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadAll(); return; }
        runAsync(() -> dao.search(kw), list -> {
            masterList.setAll(list);
            recordCountLabel.setText(list.size() + " records");
        }, "bowling-search");
    }

    @FXML private void handleAdd() {
        openModal(null).ifPresent(b -> runAsync(() -> {
            int id = dao.insert(b); b.setBookingId(id); return b;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Success", "Booking added."); }, "bowling-insert"));
    }

    private void handleEdit(BowlingBooking b) {
        openModal(b).ifPresent(updated -> runAsync(() -> {
            dao.update(updated); return updated;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Success", "Booking updated."); }, "bowling-update"));
    }

    private void handleDelete(BowlingBooking b) {
        if (!AlertHelper.showConfirm("Delete Booking", "Delete Booking #" + b.getBookingId() + "?")) return;
        runAsync(() -> { dao.delete(b.getBookingId()); return b; },
                 ignored -> { loadAll(); AlertHelper.showInfo("Deleted", "Booking removed."); }, "bowling-delete");
    }

    private Optional<BowlingBooking> openModal(BowlingBooking existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BowlingModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            BowlingModalController ctrl = loader.getController();
            ctrl.setMode(existing);

            Dialog<BowlingBooking> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(existing == null ? "Add Booking" : "Edit Booking");
            dialog.setResultConverter(b -> null);
            ctrl.setOnSave(b -> { dialog.setResult(b); dialog.close(); });
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
        task.setOnFailed(e -> Platform.runLater(() ->
            AlertHelper.showError("Error", task.getException().getMessage())));
        new Thread(task, name).start();
    }
}

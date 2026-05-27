package com.amusementpark;

import com.amusementpark.model.Ride;
import com.amusementpark.model.RideDAO;
import com.amusementpark.model.Staff;
import com.amusementpark.model.StaffDAO;

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
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class RidesController implements Initializable {

    @FXML private TextField              searchField;
    @FXML private Label                  recordCountLabel;
    @FXML private TableView<Ride>        ridesTable;
    @FXML private TableColumn<Ride, Integer> colId;
    @FXML private TableColumn<Ride, String>  colName;
    @FXML private TableColumn<Ride, Boolean> colStatus;
    @FXML private TableColumn<Ride, String>  colOperator;
    @FXML private TableColumn<Ride, Void>    colActions;

    private final RideDAO  rideDAO  = new RideDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final ObservableList<Ride> masterList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bindColumns();
        addActionsColumn();
        loadAll();
    }

    private void bindColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("rideId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("rideName"));
        colOperator.setCellValueFactory(new PropertyValueFactory<>("operatorName"));

        // Status: show "Active" / "Inactive" badge
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val ? "Active" : "Inactive");
                setStyle(val
                    ? "-fx-text-fill: #16A34A; -fx-font-weight: bold;"
                    : "-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            }
        });

        ridesTable.setItems(masterList);
    }

    private void addActionsColumn() {
        Callback<TableColumn<Ride, Void>, TableCell<Ride, Void>> factory = col -> new TableCell<>() {
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
        runAsync(rideDAO::findAll, list -> {
            masterList.setAll(list);
            recordCountLabel.setText(list.size() + " records");
        }, "rides-load");
    }

    @FXML private void handleSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadAll(); return; }
        runAsync(() -> rideDAO.search(kw), list -> {
            masterList.setAll(list);
            recordCountLabel.setText(list.size() + " records");
        }, "rides-search");
    }

    @FXML private void handleAdd() {
        openModal(null).ifPresent(ride -> runAsync(() -> {
            int id = rideDAO.insert(ride);
            ride.setRideId(id);
            return ride;
        }, ignored -> loadAll(), "rides-insert"));
    }

    private void handleEdit(Ride ride) {
        openModal(ride).ifPresent(updated -> runAsync(() -> {
            rideDAO.update(updated);
            return updated;
        }, ignored -> loadAll(), "rides-update"));
    }

    private void handleDelete(Ride ride) {
        if (!AlertHelper.showConfirm("Delete Ride",
                "Delete '" + ride.getRideName() + "'? This cannot be undone.")) return;
        runAsync(() -> { rideDAO.delete(ride.getRideId()); return ride; },
                 ignored -> { loadAll(); AlertHelper.showInfo("Deleted", ride.getRideName() + " removed."); },
                 "rides-delete");
    }

    private Optional<Ride> openModal(Ride existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RidesModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            RidesModalController ctrl = loader.getController();
            // Load staff list synchronously for the ComboBox
            List<Staff> staffList;
            try { staffList = staffDAO.findAll(); } catch (SQLException e) { staffList = List.of(); }
            ctrl.setMode(existing, staffList);

            Dialog<Ride> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(existing == null ? "Add Ride" : "Edit Ride");
            dialog.setResultConverter(b -> null);
            ctrl.setOnSave(r -> { dialog.setResult(r); dialog.close(); });
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

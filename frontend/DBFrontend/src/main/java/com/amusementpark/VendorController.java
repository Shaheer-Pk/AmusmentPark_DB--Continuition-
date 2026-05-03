package com.amusementpark;

import com.amusementpark.model.FoodOwner;
import com.amusementpark.model.FoodStall;
import com.amusementpark.model.VendorDAO;

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
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class VendorController implements Initializable {

    // ── Owner table ──────────────────────────────────────────────────────
    @FXML private TextField ownerSearchField;
    @FXML private TableView<FoodOwner>              ownersTable;
    @FXML private TableColumn<FoodOwner, Integer>   colOwnerId;
    @FXML private TableColumn<FoodOwner, String>    colOwnerFirst;
    @FXML private TableColumn<FoodOwner, String>    colOwnerLast;
    @FXML private TableColumn<FoodOwner, String>    colOwnerEmail;
    @FXML private TableColumn<FoodOwner, String>    colOwnerPhone;
    @FXML private TableColumn<FoodOwner, Void>      colOwnerActions;

    // ── Stall table ──────────────────────────────────────────────────────
    @FXML private TextField stallSearchField;
    @FXML private TableView<FoodStall>              stallsTable;
    @FXML private TableColumn<FoodStall, Integer>   colStallId;
    @FXML private TableColumn<FoodStall, String>    colStallName;
    @FXML private TableColumn<FoodStall, String>    colStallType;
    @FXML private TableColumn<FoodStall, BigDecimal> colStallRent;
    @FXML private TableColumn<FoodStall, LocalTime> colStallOpen;
    @FXML private TableColumn<FoodStall, LocalTime> colStallClose;
    @FXML private TableColumn<FoodStall, String>    colStallOwner;
    @FXML private TableColumn<FoodStall, Void>      colStallActions;

    private final VendorDAO dao = new VendorDAO();
    private final ObservableList<FoodOwner> ownerList = FXCollections.observableArrayList();
    private final ObservableList<FoodStall> stallList = FXCollections.observableArrayList();

    private static final NumberFormat PKR = NumberFormat.getNumberInstance(Locale.US);
    static { PKR.setMinimumFractionDigits(2); PKR.setMaximumFractionDigits(2); }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bindOwnerColumns();
        bindStallColumns();
        addOwnerActions();
        addStallActions();
        loadAllOwners();
        loadAllStalls();
    }

    // ── Column binding ───────────────────────────────────────────────────

    private void bindOwnerColumns() {
        colOwnerId.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        colOwnerFirst.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colOwnerLast.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colOwnerEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colOwnerPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        ownersTable.setItems(ownerList);
    }

    private void bindStallColumns() {
        colStallId.setCellValueFactory(new PropertyValueFactory<>("stallId"));
        colStallName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStallType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStallOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));

        colStallRent.setCellValueFactory(new PropertyValueFactory<>("rent"));
        colStallRent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : PKR.format(val));
            }
        });

        colStallOpen.setCellValueFactory(new PropertyValueFactory<>("openingTime"));
        colStallOpen.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalTime val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? "—" : val.toString());
            }
        });

        colStallClose.setCellValueFactory(new PropertyValueFactory<>("closingTime"));
        colStallClose.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalTime val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? "—" : val.toString());
            }
        });

        stallsTable.setItems(stallList);
    }

    // ── Action columns ────────────────────────────────────────────────────

    private void addOwnerActions() {
        Callback<TableColumn<FoodOwner, Void>, TableCell<FoodOwner, Void>> factory = col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                deleteBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e   -> handleEditOwner(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDeleteOwner(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
        colOwnerActions.setCellFactory(factory);
    }

    private void addStallActions() {
        Callback<TableColumn<FoodStall, Void>, TableCell<FoodStall, Void>> factory = col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                deleteBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e   -> handleEditStall(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDeleteStall(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
        colStallActions.setCellFactory(factory);
    }

    // ── Load ──────────────────────────────────────────────────────────────

    private void loadAllOwners() {
        runAsync(dao::findAllOwners, ownerList::setAll, "owner-load");
    }

    private void loadAllStalls() {
        runAsync(dao::findAllStalls, stallList::setAll, "stall-load");
    }

    // ── Search ────────────────────────────────────────────────────────────

    @FXML private void handleOwnerSearch() {
        String kw = ownerSearchField.getText().trim();
        if (kw.isEmpty()) { loadAllOwners(); return; }
        runAsync(() -> dao.searchOwners(kw), ownerList::setAll, "owner-search");
    }

    @FXML private void handleStallSearch() {
        String kw = stallSearchField.getText().trim();
        if (kw.isEmpty()) { loadAllStalls(); return; }
        runAsync(() -> dao.searchStalls(kw), stallList::setAll, "stall-search");
    }

    // ── Owner CRUD ────────────────────────────────────────────────────────

    @FXML private void handleAddOwner() {
        openOwnerModal(null).ifPresent(owner -> runAsync(() -> {
            int id = dao.insertOwner(owner);
            owner.setOwnerId(id);
            return owner;
        }, ignored -> { loadAllOwners(); AlertHelper.showInfo("Success", "Owner added."); }, "owner-insert"));
    }

    private void handleEditOwner(FoodOwner owner) {
        openOwnerModal(owner).ifPresent(updated -> runAsync(() -> {
            dao.updateOwner(updated);
            return updated;
        }, ignored -> { loadAllOwners(); AlertHelper.showInfo("Success", "Owner updated."); }, "owner-update"));
    }

    private void handleDeleteOwner(FoodOwner owner) {
        if (!AlertHelper.showConfirm("Delete Owner",
                "Delete " + owner.getFullName() + "?\nAll their stalls must be removed first.")) return;
        runAsync(() -> { dao.deleteOwner(owner.getOwnerId()); return owner; },
                 ignored -> { loadAllOwners(); AlertHelper.showInfo("Deleted", owner.getFullName() + " removed."); },
                 "owner-delete");
    }

    // ── Stall CRUD ────────────────────────────────────────────────────────

    @FXML private void handleAddStall() {
        openStallModal(null).ifPresent(stall -> runAsync(() -> {
            int id = dao.insertStall(stall);
            stall.setStallId(id);
            return stall;
        }, ignored -> { loadAllStalls(); AlertHelper.showInfo("Success", "Stall added."); }, "stall-insert"));
    }

    private void handleEditStall(FoodStall stall) {
        openStallModal(stall).ifPresent(updated -> runAsync(() -> {
            dao.updateStall(updated);
            return updated;
        }, ignored -> { loadAllStalls(); AlertHelper.showInfo("Success", "Stall updated."); }, "stall-update"));
    }

    private void handleDeleteStall(FoodStall stall) {
        if (!AlertHelper.showConfirm("Delete Stall",
                "Delete stall '" + stall.getName() + "'?")) return;
        runAsync(() -> { dao.deleteStall(stall.getStallId()); return stall; },
                 ignored -> { loadAllStalls(); AlertHelper.showInfo("Deleted", stall.getName() + " removed."); },
                 "stall-delete");
    }

    // ── Modals ────────────────────────────────────────────────────────────

    private Optional<FoodOwner> openOwnerModal(FoodOwner existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OwnerModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            OwnerModalController ctrl = loader.getController();
            ctrl.setMode(existing);

            Dialog<FoodOwner> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(existing == null ? "Add Owner" : "Edit Owner");
            dialog.setResultConverter(b -> null);
            ctrl.setOnSave(o -> { dialog.setResult(o); dialog.close(); });
            ctrl.setOnCancel(dialog::close);
            return dialog.showAndWait();
        } catch (IOException ex) {
            AlertHelper.showError("Modal Error", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<FoodStall> openStallModal(FoodStall existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StallModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            StallModalController ctrl = loader.getController();
            List<FoodOwner> owners;
            try { owners = dao.findAllOwners(); } catch (SQLException e) { owners = List.of(); }
            ctrl.setMode(existing, owners);

            Dialog<FoodStall> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(existing == null ? "Add Stall" : "Edit Stall");
            dialog.setResultConverter(b -> null);
            ctrl.setOnSave(s -> { dialog.setResult(s); dialog.close(); });
            ctrl.setOnCancel(dialog::close);
            return dialog.showAndWait();
        } catch (IOException ex) {
            AlertHelper.showError("Modal Error", ex.getMessage());
            return Optional.empty();
        }
    }

    // ── Generic async runner ──────────────────────────────────────────────

    @FunctionalInterface interface DbSupplier<T> { T get() throws SQLException; }

    private <T> void runAsync(DbSupplier<T> db, Consumer<T> onSuccess, String name) {
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return db.get(); } };
        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable c = task.getException();
            AlertHelper.showError("Error",
                c instanceof IllegalStateException ? c.getMessage() : "DB error: " + c.getMessage());
        }));
        new Thread(task, name).start();
    }
}

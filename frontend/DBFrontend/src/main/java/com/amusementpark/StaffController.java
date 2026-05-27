package com.amusementpark;

import com.amusementpark.model.StaffDAO;
import com.amusementpark.model.Staff;

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
import javafx.scene.layout.HBox;import javafx.util.Callback;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class StaffController implements Initializable {

    // ── FXML injections ──────────────────────────────────────────────────
    @FXML private TextField                 searchField;
    @FXML private Label                     recordCountLabel;
    @FXML private TableView<Staff>          staffTable;
    @FXML private TableColumn<Staff, Integer>    colId;
    @FXML private TableColumn<Staff, String>     colFirstName;
    @FXML private TableColumn<Staff, String>     colLastName;
    @FXML private TableColumn<Staff, String>     colTitle;
    @FXML private TableColumn<Staff, String>     colEmail;
    @FXML private TableColumn<Staff, String>     colPhone;
    @FXML private TableColumn<Staff, BigDecimal> colSalary;
    @FXML private TableColumn<Staff, Integer>    colReportsTo;
    @FXML private TableColumn<Staff, Void>       colActions;

    private final StaffDAO dao = new StaffDAO();
    private final ObservableList<Staff> masterList = FXCollections.observableArrayList();

    private static final NumberFormat PKR = NumberFormat.getNumberInstance(Locale.US);
    static {
        PKR.setMinimumFractionDigits(2);
        PKR.setMaximumFractionDigits(2);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bindColumns();
        addActionsColumn();
        loadAllStaff();
    }

    // ── Column binding ───────────────────────────────────────────────────

    private void bindColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("staffId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        // Salary: format as "PKR X,XXX.00"
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colSalary.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : PKR.format(val));
            }
        });

        // ReportsTo: show ID or "—" if null (top-level manager)
        colReportsTo.setCellValueFactory(new PropertyValueFactory<>("reportsTo"));
        colReportsTo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? "—" : "#" + val);
            }
        });

        staffTable.setItems(masterList);
    }

    // ── Actions column: Edit + Delete buttons per row ────────────────────

    private void addActionsColumn() {
        Callback<TableColumn<Staff, Void>, TableCell<Staff, Void>> factory = col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                deleteBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Staff staff = getTableView().getItems().get(getIndex());
                    handleEdit(staff);
                });

                deleteBtn.setOnAction(e -> {
                    Staff staff = getTableView().getItems().get(getIndex());
                    handleDelete(staff);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };

        colActions.setCellFactory(factory);
    }

    // ── Data loading ─────────────────────────────────────────────────────

    private void loadAllStaff() {
        runAsync(dao::findAll, this::populateTable, "staff-load-thread");
    }

    private void populateTable(List<Staff> list) {
        masterList.setAll(list);
        recordCountLabel.setText(list.size() + " record" + (list.size() == 1 ? "" : "s"));
    }

    // ── Search ───────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAllStaff();
            return;
        }
        runAsync(() -> dao.search(keyword), this::populateTable, "staff-search-thread");
    }

    // ── Add ──────────────────────────────────────────────────────────────

    @FXML
    private void handleAdd() {
        Optional<Staff> result = openModal(null);
        result.ifPresent(newStaff -> runAsync(() -> {
            int id = dao.insert(newStaff);
            newStaff.setStaffId(id);
            return newStaff;
        }, ignored -> {
            loadAllStaff();
            AlertHelper.showInfo("Success", "Staff member added successfully.");
        }, "staff-insert-thread"));
    }

    // ── Edit ─────────────────────────────────────────────────────────────

    private void handleEdit(Staff staff) {
        Optional<Staff> result = openModal(staff);
        result.ifPresent(updated -> runAsync(() -> {
            dao.update(updated);
            return updated;
        }, ignored -> {
            loadAllStaff();
            AlertHelper.showInfo("Success", "Staff member updated successfully.");
        }, "staff-update-thread"));
    }

    // ── Delete (FK-safe) ─────────────────────────────────────────────────

    private void handleDelete(Staff staff) {
        boolean confirmed = AlertHelper.showConfirm(
            "Delete Staff",
            "Delete " + staff.getFullName() + "?\n\n" +
            "Job assignments will be removed automatically.\n" +
            "If they manage other staff, those staff will become top-level."
        );
        if (!confirmed) return;

        runAsync(() -> {
            dao.delete(staff.getStaffId());
            return staff;
        }, ignored -> {
            loadAllStaff();
            AlertHelper.showInfo("Deleted", staff.getFullName() + " has been removed.");
        }, "staff-delete-thread");
    }

    // ── Modal ─────────────────────────────────────────────────────────────

    /**
     * Opens the Staff Add/Edit modal as a JavaFX Dialog.
     * Passes existing staff for Edit (null for Add).
     * Returns Optional<Staff> with the filled-in data, or empty if cancelled.
     */
    private Optional<Staff> openModal(Staff existing) {
        try {
            URL url1 = getClass().getResource("/fxml/StaffModal.fxml");
            URL url2 = getClass().getResource("/css/style.css");

            FXMLLoader loader = new FXMLLoader(url1);
            DialogPane dialogPane = new DialogPane();
            dialogPane.setContent(loader.load());
            dialogPane.getStylesheets().add(url2.toExternalForm());
            // Hide default dialog buttons — our modal has its own footer
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.lookupButton(ButtonType.OK).setVisible(false);
            dialogPane.lookupButton(ButtonType.CANCEL).setVisible(false);

            StaffModalController modalCtrl = loader.getController();
            modalCtrl.setMode(existing, masterList.stream().toList());

            Dialog<Staff> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(existing == null ? "Add Staff Member" : "Edit Staff Member");

            // The modal controller signals completion via a result callback
            dialog.setResultConverter(btn -> null); // default — modal manages its own close
            modalCtrl.setOnSave(staff -> {
                dialog.setResult(staff);
                dialog.close();
            });
            modalCtrl.setOnCancel(dialog::close);

            return dialog.showAndWait();

        } catch (IOException ex) {
            AlertHelper.showError("Modal Error", "Could not open staff form: " + ex.getMessage());
            return Optional.empty();
        }
    }

    // ── Generic async runner ──────────────────────────────────────────────

    /**
     * Runs a DB call off the FX thread, then calls the success handler on the FX thread.
     * Shows an error dialog on failure. Generic enough for any DAO result type.
     */
    @FunctionalInterface
    interface DbSupplier<T> { T get() throws SQLException; }

    private <T> void runAsync(DbSupplier<T> dbCall,
                               Consumer<T> onSuccess,
                               String threadName) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return dbCall.get();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.accept(task.getValue())));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable cause = task.getException();
            String msg = (cause instanceof IllegalStateException)
                ? cause.getMessage()                        // FK guard message — show as-is
                : "Database error: " + cause.getMessage();  // raw SQL error
            AlertHelper.showError("Operation Failed", msg);
        }));

        new Thread(task, threadName).start();
    }
}

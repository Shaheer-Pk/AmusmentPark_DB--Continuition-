package com.amusementpark.controller;

import com.amusementpark.dao.RideDAO;
import com.amusementpark.model.Card;
import com.amusementpark.model.Ride;
import com.amusementpark.model.RideOperatorAssignment;
import com.amusementpark.session.SessionManager;
import com.amusementpark.util.AlertHelper;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// ─────────────────────────────────────────────────────────────────────────────
// RidesPanelController  —  permission-sculpted rides panel.
//
// ONE FXML, ONE CONTROLLER, MULTIPLE VIEWS.
//
// PERMISSION → UI MAP:
//   VIEW_RIDES             → rideTable (panel entry gate)
//   VIEW_ASSIGNED_RIDES    → loadRides() routes to getAssignedRides(staffID)
//   PURCHASE_RIDE          → customerActionBar
//   UPDATE_RIDE_STATUS     → operatorActionBar
//   UPDATE_RIDE            → editRideButton
//   DELETE_RIDE            → deleteRideButton
//   CREATE_RIDE            → managementToolbar
//   ASSIGN_RIDE_OPERATOR   → assignOperatorButton
//
// TABLE COLUMNS (V2):
//   colName | colPrice | colStatus
//   colOperator REMOVED — operator data loaded on demand via dialog only.
//
// OPERATOR ASSIGNMENT DIALOG:
//   Section 1 — currently assigned operators with Remove buttons.
//     Remove ENABLED  → operator.isRemovable() (reports to this manager)
//     Remove DISABLED → operator reports to another manager + tooltip
//   Section 2 — assignable operators (report to this manager, not on this ride).
//     Shows name + workload count. Assign button per row.
//   Both sections refresh in-dialog after each action without closing.
//
// THREADING:
//   All DAO calls on daemon threads via Task.
//   All UI updates on FX thread via onSucceeded/onFailed.
// ─────────────────────────────────────────────────────────────────────────────
public class RidesPanelController {

    // ── FXML: Page header ─────────────────────────────────────────────────────
    @FXML private Label  statusLabel;
    @FXML private Button refreshButton;

    // ── FXML: Management toolbar (CREATE_RIDE) ────────────────────────────────
    @FXML private HBox      managementToolbar;
    @FXML private TextField newRideNameField;
    @FXML private TextField newRidePriceField;
    @FXML private Button    createRideButton;

    // ── FXML: Main ride table ─────────────────────────────────────────────────
    @FXML private TableView<Ride>          rideTable;
    @FXML private TableColumn<Ride,String> colName;
    @FXML private TableColumn<Ride,String> colPrice;
    @FXML private TableColumn<Ride,String> colStatus;

    // ── FXML: Action bars ─────────────────────────────────────────────────────
    @FXML private HBox   customerActionBar;
    @FXML private Button purchaseButton;

    @FXML private HBox   operatorActionBar;
    @FXML private Button markOperationalBtn;
    @FXML private Button markOfflineBtn;

    @FXML private HBox   managerActionBar;
    @FXML private Button editRideButton;
    @FXML private Button deleteRideButton;
    @FXML private Button assignOperatorButton;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final RideDAO        rideDAO = new RideDAO();
    private final SessionManager session = SessionManager.getInstance();

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    private void initialize() {

        colName.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getRideName()));
        colPrice.setCellValueFactory(cd ->
            new SimpleStringProperty("PKR " + cd.getValue().getRidePrice().toPlainString()));
        colStatus.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStatusDisplay()));

        setVisible(managementToolbar,
            session.hasPermission("CREATE_RIDE"));
        setVisible(customerActionBar,
            session.hasPermission("PURCHASE_RIDE"));
        setVisible(operatorActionBar,
            session.hasPermission("UPDATE_RIDE_STATUS"));
        setVisible(managerActionBar,
            session.hasAnyPermission("UPDATE_RIDE", "DELETE_RIDE", "ASSIGN_RIDE_OPERATOR"));

        setVisible(editRideButton,       session.hasPermission("UPDATE_RIDE"));
        setVisible(deleteRideButton,     session.hasPermission("DELETE_RIDE"));
        setVisible(assignOperatorButton, session.hasPermission("ASSIGN_RIDE_OPERATOR"));

        loadRides();
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() { loadRides(); }

    private void loadRides() {
        statusLabel.setText("Loading...");
        rideTable.setItems(FXCollections.emptyObservableList());

        boolean isOperator = session.hasPermission("VIEW_ASSIGNED_RIDES");
        int     staffID    = session.getStaffID();

        // StaffID is set to -1 by default if authorized user doesnt have role 'Staff'
        // See sessionManager.java for more info
        if (isOperator && staffID == -1) {
            statusLabel.setText("Error: staff record not found.");
            AlertHelper.showError("Configuration Error",
                "Your account has operator permissions but no Staff record was found.\n"
                + "Please contact your administrator.");
            return;
        }

        Task<List<Ride>> task = new Task<>() {
            @Override
            protected List<Ride> call() throws SQLException {
                return isOperator
                    ? rideDAO.getAssignedRides(staffID)         // The re-routing for table View in case a customer/manager/admin OR operator is viewing
                    : rideDAO.getAllRides();
            }
        };

        task.setOnSucceeded(e -> {
            List<Ride> rides = task.getValue();
            rideTable.setItems(FXCollections.observableArrayList(rides));
            statusLabel.setText(rides.size() + " ride" + (rides.size() == 1 ? "" : "s") + " loaded.");
        });

        task.setOnFailed(e -> {
            statusLabel.setText("Failed to load rides.");
            AlertHelper.showError("Load Failed", "Could not load ride data.");
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    @FXML
    private void handlePurchase() {
        Ride selected = rideTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a ride to purchase.");
            return;
        }
        if (!selected.isOperational()) {
            AlertHelper.showError("Ride Unavailable",
                selected.getRideName() + " is currently offline.");
            return;
        }

        Card card = session.getCard();
        if (card == null) {
            String roleDisplay = String.join(", ", session.getRoles());
            AlertHelper.showError("Purchase Not Allowed",
                "Your role (" + roleDisplay + ") is not permitted to purchase rides.\n"
                + "Please log in from a Customer account.");
            return;
        }

        if (!AlertHelper.showConfirm("Confirm Purchase",
                "Purchase " + selected.getRideName()
                + " for PKR " + selected.getRidePrice().toPlainString() + "?")) return;

        int cardID = card.getCardID();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                rideDAO.purchaseRide(cardID, selected.getRideID());
                return null;
            }
        };

        task.setOnSucceeded(e ->
            AlertHelper.showInfo("Purchase Successful",
                "Enjoy your ride on " + selected.getRideName() + "!"));
        task.setOnFailed(e -> {
            AlertHelper.showError("Purchase Failed", task.getException().getMessage());
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    // ── Status Toggle ─────────────────────────────────────────────────────────

    @FXML private void handleMarkOperational() { setSelectedRideStatus(true);  }
    @FXML private void handleMarkOffline()     { setSelectedRideStatus(false); }

    private void setSelectedRideStatus(boolean operational) {
        Ride selected = rideTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a ride first.");
            return;
        }
        if (selected.isOperational() == operational) {
            AlertHelper.showInfo("No Change",
                selected.getRideName() + " is already "
                + (operational ? "operational." : "offline."));
            return;
        }
        if (!AlertHelper.showConfirm("Confirm",
                "Are you sure you want to "
                + (operational ? "mark operational" : "take offline")
                + " " + selected.getRideName() + "?")) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                rideDAO.setRideStatus(selected.getRideID(), operational);
                return null;
            }
        };

        task.setOnSucceeded(e -> loadRides());
        task.setOnFailed(e -> {
            AlertHelper.showError("Update Failed", "Could not update ride status.");
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    // ── Create Ride ───────────────────────────────────────────────────────────

    @FXML
    private void handleCreateRide() {
        String name      = newRideNameField.getText().trim();
        String priceText = newRidePriceField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            AlertHelper.showError("Missing Fields",
                "Please enter both a ride name and price.");
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
            if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            AlertHelper.showError("Invalid Price",
                "Please enter a valid positive number for price.");
            return;
        }

        final BigDecimal finalPrice = price;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                rideDAO.createRide(name, finalPrice);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            newRideNameField.clear();
            newRidePriceField.clear();
            AlertHelper.showInfo("Ride Created", name + " has been added.");
            loadRides();
        });
        task.setOnFailed(e -> {
            AlertHelper.showError("Create Failed", "Could not create ride.");
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    // ── Edit Ride ─────────────────────────────────────────────────────────────

    @FXML
    private void handleEditRide() {
        Ride selected = rideTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a ride to edit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Ride");
        dialog.setHeaderText("Editing: " + selected.getRideName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField  = new TextField(selected.getRideName());
        TextField priceField = new TextField(selected.getRidePrice().toPlainString());
        nameField.setPromptText("Ride name");
        priceField.setPromptText("Price (PKR)");

        VBox content = new VBox(10,
            new Label("Ride Name:"), nameField,
            new Label("Price (PKR):"), priceField);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) return;

        String newName      = nameField.getText().trim();
        String newPriceText = priceField.getText().trim();

        if (newName.isEmpty() || newPriceText.isEmpty()) {
            AlertHelper.showError("Missing Fields", "Name and price cannot be empty.");
            return;
        }

        BigDecimal newPrice;
        try {
            newPrice = new BigDecimal(newPriceText);
            if (newPrice.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            AlertHelper.showError("Invalid Price",
                "Please enter a valid positive number for price.");
            return;
        }

        final BigDecimal finalPrice = newPrice;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                rideDAO.updateRide(selected.getRideID(), newName, finalPrice);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            AlertHelper.showInfo("Ride Updated", newName + " has been updated.");
            loadRides();
        });
        task.setOnFailed(e -> {
            AlertHelper.showError("Update Failed", "Could not update ride.");
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    // ── Delete Ride ───────────────────────────────────────────────────────────

    @FXML
    private void handleDeleteRide() {
        Ride selected = rideTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a ride to delete.");
            return;
        }
        if (!AlertHelper.showConfirm("Confirm Delete",
                "Permanently delete " + selected.getRideName() + "?\n"
                + "This cannot be undone.")) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                rideDAO.deleteRide(selected.getRideID());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            AlertHelper.showInfo("Deleted", selected.getRideName() + " has been removed.");
            loadRides();
        });
        task.setOnFailed(e -> {
            AlertHelper.showError("Delete Failed",
                "Could not delete ride. It may have active usage records.");
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    // ── View / Edit Assigned Operators ────────────────────────────────────────

    @FXML
    private void handleViewEditOperators() {
        Ride selected = rideTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection",
                "Please select a ride to manage operators.");
            return;
        }

        int managerStaffID = session.getStaffID();
        if (managerStaffID == -1) {
            AlertHelper.showError("Configuration Error",
                "No Staff record found for your account. Contact your administrator.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Operators — " + selected.getRideName());
        dialog.setHeaderText("Assigned operators for: " + selected.getRideName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(440);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        dialog.getDialogPane().setContent(scrollPane);

        refreshDialogContent(scrollPane, selected.getRideID(), managerStaffID);

        dialog.showAndWait();
    }

    /**
     * Fetches both operator lists on a background thread and rebuilds
     * the dialog content. Called on open and after every assign/remove action.
     */
    private void refreshDialogContent(ScrollPane scrollPane,
                                      int rideID, int managerStaffID) {
        Label loading = new Label("Loading...");
        loading.setPadding(new Insets(20));
        scrollPane.setContent(loading);

        Task<DialogData> task = new Task<>() {
            @Override
            protected DialogData call() throws SQLException {
                return new DialogData(
                    rideDAO.getOperatorsForRide(rideID, managerStaffID),
                    rideDAO.getAssignableOperators(rideID, managerStaffID)
                );
            }
        };

        task.setOnSucceeded(e ->
            scrollPane.setContent(
                buildDialogContent(scrollPane, task.getValue(), rideID, managerStaffID)));

        task.setOnFailed(e -> {
            Label err = new Label("Failed to load operator data. Please close and retry.");
            err.setPadding(new Insets(20));
            scrollPane.setContent(err);
            task.getException().printStackTrace();
        });

        startDaemonTask(task);
    }

    /**
     * Builds the full dialog VBox from fetched DialogData.
     * Section 1: assigned operators with conditional Remove buttons.
     * Section 2: assignable operators with Assign buttons + workload info.
     */
    private VBox buildDialogContent(ScrollPane scrollPane, DialogData data,
                                    int rideID, int managerStaffID) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(16));

        // ── Section 1: Currently Assigned ─────────────────────────────────────
        Label assignedTitle = new Label("Currently Assigned Operators");
        assignedTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        root.getChildren().add(assignedTitle);

        if (data.assigned.isEmpty()) {
            root.getChildren().add(
                new Label("No operators currently assigned to this ride."));
        } else {
            for (RideOperatorAssignment op : data.assigned) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));

                Label name = new Label(op.getOperatorName());
                HBox.setHgrow(name, Priority.ALWAYS);

                Button removeBtn = new Button("Remove");
                removeBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; "
                    + "-fx-cursor: hand;");

                if (op.isRemovable()) {
                    removeBtn.setOnAction(evt -> {
                        Task<Void> t = new Task<>() {
                            @Override protected Void call() throws SQLException {
                                rideDAO.removeOperatorFromRide(op.getStaffID(), rideID);
                                return null;
                            }
                        };
                        t.setOnSucceeded(ev ->
                            refreshDialogContent(scrollPane, rideID, managerStaffID));
                        t.setOnFailed(ev -> {
                            AlertHelper.showError("Remove Failed",
                                "Could not remove operator. Please try again.");
                            t.getException().printStackTrace();
                        });
                        startDaemonTask(t);
                    });
                } else {
                    removeBtn.setDisable(true);
                    removeBtn.setStyle("-fx-background-color: #bdbdbd; -fx-text-fill: white;");
                    Tooltip.install(removeBtn,
                        new Tooltip("This operator reports to another manager."));
                }

                row.getChildren().addAll(name, removeBtn);
                root.getChildren().add(row);
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setPadding(new Insets(6, 0, 6, 0));
        root.getChildren().add(sep);

        // ── Section 2: Add Operator ───────────────────────────────────────────
        Label addTitle = new Label("Add Operator");
        addTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        root.getChildren().add(addTitle);

        if (data.assignable.isEmpty()) {
            root.getChildren().add(new Label(
                "No available operators to assign.\n"
                + "All operators reporting to you may already be on this ride,\n"
                + "or you have no operators assigned to you yet."));
        } else {
            for (RideOperatorAssignment op : data.assignable) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));

                String workload = op.getTotalAssigned() == 0
                    ? "No other rides"
                    : op.getTotalAssigned() + " other ride"
                      + (op.getTotalAssigned() == 1 ? "" : "s");
                Label name = new Label(op.getOperatorName() + "   (" + workload + ")");
                name.setStyle("-fx-text-fill: #444;");
                HBox.setHgrow(name, Priority.ALWAYS);

                Button assignBtn = new Button("Assign");
                assignBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; "
                    + "-fx-cursor: hand;");
                assignBtn.setOnAction(evt -> {
                    Task<Void> t = new Task<>() {
                        @Override protected Void call() throws SQLException {
                            rideDAO.assignOperatorToRide(op.getStaffID(), rideID);
                            return null;
                        }
                    };
                    t.setOnSucceeded(ev ->
                        refreshDialogContent(scrollPane, rideID, managerStaffID));
                    t.setOnFailed(ev -> {
                        AlertHelper.showError("Assign Failed",
                            "Could not assign operator. Please try again.");
                        t.getException().printStackTrace();
                    });
                    startDaemonTask(t);
                });

                row.getChildren().addAll(name, assignBtn);
                root.getChildren().add(row);
            }
        }

        return root;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void startDaemonTask(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Data Carrier ──────────────────────────────────────────────────────────

    private static class DialogData {
        final List<RideOperatorAssignment> assigned;
        final List<RideOperatorAssignment> assignable;

        DialogData(List<RideOperatorAssignment> assigned,
                   List<RideOperatorAssignment> assignable) {
            this.assigned   = assigned;
            this.assignable = assignable;
        }
    }
}
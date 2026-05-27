package com.amusementpark;

import com.amusementpark.model.CustomerCard;
import com.amusementpark.model.CustomerDAO;

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
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CRMController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label     recordCountLabel;
    @FXML private TableView<CustomerCard>               crmTable;
    @FXML private TableColumn<CustomerCard, Integer>    colId;
    @FXML private TableColumn<CustomerCard, String>     colFirstName;
    @FXML private TableColumn<CustomerCard, String>     colLastName;
    @FXML private TableColumn<CustomerCard, String>     colType;
    @FXML private TableColumn<CustomerCard, LocalDate>  colDOB;
    @FXML private TableColumn<CustomerCard, Integer>    colCardId;
    @FXML private TableColumn<CustomerCard, BigDecimal> colBalance;
    @FXML private TableColumn<CustomerCard, Integer>    colPoints;
    @FXML private TableColumn<CustomerCard, Void>       colActions;

    private final CustomerDAO dao = new CustomerDAO();
    private final ObservableList<CustomerCard> masterList = FXCollections.observableArrayList();
    private static final NumberFormat PKR = NumberFormat.getNumberInstance(Locale.US);
    static { PKR.setMinimumFractionDigits(2); PKR.setMaximumFractionDigits(2); }

    @Override public void initialize(URL url, ResourceBundle rb) {
        bindColumns(); addActionsColumn(); loadAll();
    }

    private void bindColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDOB.setCellValueFactory(new PropertyValueFactory<>("dob"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));

        colCardId.setCellValueFactory(new PropertyValueFactory<>("cardId"));
        colCardId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? "—" : "#" + val);
            }
        });

        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colBalance.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? "—" : PKR.format(val));
            }
        });

        crmTable.setItems(masterList);
    }

    private void addActionsColumn() {
        Callback<TableColumn<CustomerCard, Void>, TableCell<CustomerCard, Void>> factory = col -> new TableCell<>() {
            private final Button editBtn     = new Button("Edit");
            private final Button rechargeBtn = new Button("Recharge");
            private final Button deleteBtn   = new Button("Delete");
            private final HBox   box         = new HBox(4, editBtn, rechargeBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                rechargeBtn.getStyleClass().add("btn-secondary");
                rechargeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 8 5 8;");
                deleteBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e     -> handleEdit(getTableView().getItems().get(getIndex())));
                rechargeBtn.setOnAction(e -> handleRecharge(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e   -> handleDelete(getTableView().getItems().get(getIndex())));
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
        }, "crm-load");
    }

    @FXML private void handleSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadAll(); return; }
        runAsync(() -> dao.search(kw), list -> {
            masterList.setAll(list);
            recordCountLabel.setText(list.size() + " records");
        }, "crm-search");
    }

    @FXML private void handleAdd() {
        openCustomerModal(null).ifPresent(cc -> runAsync(() -> {
            dao.insertWithCard(cc); return cc;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Success", "Customer added with card."); }, "crm-insert"));
    }

    private void handleEdit(CustomerCard cc) {
        openCustomerModal(cc).ifPresent(updated -> runAsync(() -> {
            dao.updateCustomer(updated); return updated;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Success", "Customer updated."); }, "crm-update"));
    }

    private void handleRecharge(CustomerCard cc) {
        if (cc.getCardId() == null) {
            AlertHelper.showError("No Card", "This customer has no linked card yet."); return;
        }
        openRechargeModal(cc).ifPresent(result -> runAsync(() -> {
            dao.rechargeCard(result[0].intValue(), new BigDecimal(result[1].toString()), result[2].intValue());
            return cc;
        }, ignored -> { loadAll(); AlertHelper.showInfo("Recharged", "Card balance updated."); }, "crm-recharge"));
    }

    private void handleDelete(CustomerCard cc) {
        if (!AlertHelper.showConfirm("Delete Customer",
                "Delete " + cc.getFullName() + "? Their card will also be deleted.")) return;
        runAsync(() -> { dao.delete(cc.getCustomerId()); return cc; },
                 ignored -> { loadAll(); AlertHelper.showInfo("Deleted", cc.getFullName() + " removed."); },
                 "crm-delete");
    }

    private Optional<CustomerCard> openCustomerModal(CustomerCard existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CRMModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            CRMModalController ctrl = loader.getController();
            ctrl.setMode(existing);

            Dialog<CustomerCard> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(existing == null ? "Add Customer" : "Edit Customer");
            dialog.setResultConverter(b -> null);
            ctrl.setOnSave(cc -> { dialog.setResult(cc); dialog.close(); });
            ctrl.setOnCancel(dialog::close);
            return dialog.showAndWait();
        } catch (IOException ex) {
            AlertHelper.showError("Modal Error", ex.getMessage());
            return Optional.empty();
        }
    }

    // Returns [cardId, topUpAmount, addPoints] as Number array
    private Optional<Number[]> openRechargeModal(CustomerCard cc) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CardRechargeModal.fxml"));
            DialogPane pane = new DialogPane();
            pane.setContent(loader.load());
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            pane.lookupButton(ButtonType.OK).setVisible(false);
            pane.lookupButton(ButtonType.CANCEL).setVisible(false);

            CardRechargeController ctrl = loader.getController();
            ctrl.setCard(cc);

            Dialog<Number[]> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Recharge Card — " + cc.getFullName());
            dialog.setResultConverter(b -> null);
            ctrl.setOnRecharge(result -> { dialog.setResult(result); dialog.close(); });
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

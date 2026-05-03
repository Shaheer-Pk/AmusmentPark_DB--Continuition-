package com.amusementpark;

import com.amusementpark.model.CustomerCard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CRMModalController implements Initializable {

    @FXML private Label      modalTitle;
    @FXML private Label      modalSubtitle;
    @FXML private TextField  firstNameField;
    @FXML private TextField  lastNameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField  dobField;
    @FXML private TextField  balanceField;
    @FXML private TextField  pointsField;
    @FXML private Label      validationLabel;
    @FXML private Button     saveButton;

    private Consumer<CustomerCard> onSave;
    private Runnable               onCancel;
    private CustomerCard           existing;

    @Override public void initialize(URL url, ResourceBundle rb) {
        typeCombo.getItems().addAll("Regular", "VIP", "Student", "Senior", "Family");
        typeCombo.setValue("Regular");
    }

    public void setMode(CustomerCard cc) {
        this.existing = cc;
        if (cc == null) {
            modalTitle.setText("Add Customer");
            modalSubtitle.setText("Fill in all required fields (*)");
        } else {
            modalTitle.setText("Edit Customer");
            modalSubtitle.setText("Editing: " + cc.getFullName());
            saveButton.setText("Save Changes");
            firstNameField.setText(cc.getFirstName());
            lastNameField.setText(cc.getLastName());
            typeCombo.setValue(cc.getType());
            dobField.setText(cc.getDob() != null ? cc.getDob().toString() : "");
            balanceField.setText(cc.getBalance() != null ? cc.getBalance().toPlainString() : "0");
            pointsField.setText(cc.getPoints() != null ? cc.getPoints().toString() : "0");
        }
    }

    public void setOnSave(Consumer<CustomerCard> cb) { this.onSave   = cb; }
    public void setOnCancel(Runnable cb)             { this.onCancel = cb; }

    @FXML private void handleSave() {
        validationLabel.setText("");
        String fn   = firstNameField.getText().trim();
        String ln   = lastNameField.getText().trim();
        String type = typeCombo.getValue();
        String dobs = dobField.getText().trim();
        String bals = balanceField.getText().trim();
        String pts  = pointsField.getText().trim();

        if (fn.isEmpty() || ln.isEmpty() || type == null) {
            validationLabel.setText("First name, last name, and type are required."); return;
        }

        LocalDate dob = null;
        if (!dobs.isEmpty()) {
            try { dob = LocalDate.parse(dobs); }
            catch (DateTimeParseException e) { validationLabel.setText("Date of Birth must be YYYY-MM-DD."); return; }
        }

        BigDecimal balance = BigDecimal.ZERO;
        if (!bals.isEmpty()) {
            try { balance = new BigDecimal(bals); }
            catch (NumberFormatException e) { validationLabel.setText("Balance must be a valid number."); return; }
        }

        int points = 0;
        if (!pts.isEmpty()) {
            try { points = Integer.parseInt(pts); }
            catch (NumberFormatException e) { validationLabel.setText("Points must be a whole number."); return; }
        }

        CustomerCard result = existing != null ? existing : new CustomerCard();
        result.setFirstName(fn);
        result.setLastName(ln);
        result.setType(type);
        result.setDob(dob);
        result.setBalance(balance);
        result.setPoints(points);

        if (onSave != null) onSave.accept(result);
    }

    @FXML private void handleCancel() { if (onCancel != null) onCancel.run(); }
}

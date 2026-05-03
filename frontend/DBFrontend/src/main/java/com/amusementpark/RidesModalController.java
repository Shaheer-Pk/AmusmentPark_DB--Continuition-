package com.amusementpark;

import com.amusementpark.model.Ride;
import com.amusementpark.model.Staff;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class RidesModalController implements Initializable {

    @FXML private Label  modalTitle;
    @FXML private Label  modalSubtitle;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<Staff>  operatorCombo;
    @FXML private Label  validationLabel;
    @FXML private Button saveButton;

    private Consumer<Ride> onSave;
    private Runnable       onCancel;
    private Ride           existing;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusCombo.getItems().addAll("Active", "Inactive");
        statusCombo.setValue("Active");

        StringConverter<Staff> conv = new StringConverter<>() {
            @Override public String toString(Staff s) { return s == null ? "" : s.getFullName() + " — " + s.getTitle(); }
            @Override public Staff fromString(String s) { return null; }
        };
        operatorCombo.setConverter(conv);
    }

    public void setMode(Ride ride, List<Staff> staffList) {
        this.existing = ride;
        operatorCombo.getItems().setAll(staffList);

        if (ride == null) {
            modalTitle.setText("Add Ride");
            modalSubtitle.setText("Fill in all fields below");
        } else {
            modalTitle.setText("Edit Ride");
            modalSubtitle.setText("Editing: " + ride.getRideName());
            saveButton.setText("Save Changes");
            nameField.setText(ride.getRideName());
            statusCombo.setValue(ride.isStatus() ? "Active" : "Inactive");
            if (ride.getOperatorId() != null) {
                staffList.stream().filter(s -> s.getStaffId() == ride.getOperatorId())
                    .findFirst().ifPresent(operatorCombo::setValue);
            }
        }
    }

    public void setOnSave(Consumer<Ride> cb)  { this.onSave   = cb; }
    public void setOnCancel(Runnable cb)       { this.onCancel = cb; }

    @FXML private void handleSave() {
        validationLabel.setText("");
        String name = nameField.getText().trim();
        if (name.isEmpty()) { validationLabel.setText("Ride name is required."); return; }

        Ride result = existing != null ? existing : new Ride();
        result.setRideName(name);
        result.setStatus("Active".equals(statusCombo.getValue()));
        Staff op = operatorCombo.getValue();
        result.setOperatorId(op != null ? op.getStaffId() : null);
        result.setOperatorName(op != null ? op.getFullName() : "—");

        if (onSave != null) onSave.accept(result);
    }

    @FXML private void handleCancel() { if (onCancel != null) onCancel.run(); }
}

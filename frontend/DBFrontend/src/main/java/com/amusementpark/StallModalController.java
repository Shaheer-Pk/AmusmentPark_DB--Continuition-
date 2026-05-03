package com.amusementpark;

import com.amusementpark.model.FoodOwner;
import com.amusementpark.model.FoodStall;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for StallModal.fxml.
 *
 * Usage (from VendorController):
 *   ctrl.setMode(null,   owners) → Add mode  (blank form, owner list populated)
 *   ctrl.setMode(stall,  owners) → Edit mode (pre-filled)
 *   ctrl.setOnSave(stall -> …)   → called with the built FoodStall on Save
 *   ctrl.setOnCancel(dialog::close)
 */
public class StallModalController implements Initializable {

    // Stall type options — extend as needed
    private static final List<String> STALL_TYPES = List.of(
        "Fast Food", "Beverages", "Desserts", "Snacks",
        "Asian", "BBQ", "Bakery", "Other"
    );

    @FXML private TextField  nameField;
    @FXML private ComboBox<String>     typeCombo;
    @FXML private TextField  rentField;
    @FXML private ComboBox<FoodOwner>  ownerCombo;
    @FXML private DatePicker establishDatePicker;
    @FXML private TextField  openingTimeField;
    @FXML private TextField  closingTimeField;
    @FXML private Label      errorLabel;

    private FoodStall existing;

    private Consumer<FoodStall> onSave;
    private Runnable            onCancel;

    // ── Initializable ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.setItems(FXCollections.observableArrayList(STALL_TYPES));

        // Show owner full name in the combo
        ownerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(FoodOwner o)    { return o == null ? "" : o.getFullName(); }
            @Override public FoodOwner fromString(String s)  { return null; }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * @param stall  {@code null} for Add mode, existing object for Edit mode.
     * @param owners Full list of available owners for the combo box.
     */
    public void setMode(FoodStall stall, List<FoodOwner> owners) {
        this.existing = stall;
        ownerCombo.setItems(FXCollections.observableArrayList(owners));

        if (stall != null) {
            nameField.setText(stall.getName());
            typeCombo.setValue(stall.getType());
            rentField.setText(stall.getRent() != null ? stall.getRent().toPlainString() : "");
            establishDatePicker.setValue(stall.getEstablishDate());
            openingTimeField.setText(stall.getOpeningTime() != null ? stall.getOpeningTime().toString() : "");
            closingTimeField.setText(stall.getClosingTime() != null ? stall.getClosingTime().toString() : "");

            // Pre-select the matching owner
            owners.stream()
                  .filter(o -> o.getOwnerId() == stall.getOwnerId())
                  .findFirst()
                  .ifPresent(ownerCombo::setValue);
        }
    }

    public void setOnSave(Consumer<FoodStall> callback) { this.onSave   = callback; }
    public void setOnCancel(Runnable callback)           { this.onCancel = callback; }

    // ── Handlers ──────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideError();

        // ── Read fields ───────────────────────────────────────────────────
        String      nameVal  = nameField.getText().trim();
        String      typeVal  = typeCombo.getValue();
        String      rentStr  = rentField.getText().trim();
        FoodOwner   owner    = ownerCombo.getValue();
        LocalDate   estDate  = establishDatePicker.getValue();
        String      openStr  = openingTimeField.getText().trim();
        String      closeStr = closingTimeField.getText().trim();

        // ── Validation ────────────────────────────────────────────────────
        if (nameVal.isEmpty()) { showError("Stall name is required."); return; }
        if (typeVal == null)   { showError("Please select a stall type."); return; }
        if (owner  == null)    { showError("Please select an owner."); return; }

        BigDecimal rent;
        try {
            rent = new BigDecimal(rentStr);
            if (rent.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Rent must be a valid non-negative number.");
            return;
        }

        LocalTime openTime = null, closeTime = null;
        if (!openStr.isEmpty()) {
            openTime = parseTime(openStr);
            if (openTime == null) { showError("Opening time must be in HH:MM format."); return; }
        }
        if (!closeStr.isEmpty()) {
            closeTime = parseTime(closeStr);
            if (closeTime == null) { showError("Closing time must be in HH:MM format."); return; }
        }
        if (openTime != null && closeTime != null && !closeTime.isAfter(openTime)) {
            showError("Closing time must be after opening time.");
            return;
        }

        // ── Build / update model ──────────────────────────────────────────
        FoodStall result = (existing != null) ? existing : new FoodStall();
        result.setName(nameVal);
        result.setType(typeVal);
        result.setRent(rent);
        result.setOwnerId(owner.getOwnerId());
        result.setOwnerName(owner.getFullName());
        result.setEstablishDate(estDate);
        result.setOpeningTime(openTime);
        result.setClosingTime(closeTime);

        if (onSave != null) onSave.accept(result);
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) onCancel.run();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Parses "H:MM" or "HH:MM", returns null on failure. */
    private LocalTime parseTime(String s) {
        try {
            // Normalise to HH:MM so LocalTime.parse works
            if (s.matches("^\\d:\\d{2}$")) s = "0" + s;
            return LocalTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}

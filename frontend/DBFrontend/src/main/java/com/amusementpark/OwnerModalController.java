package com.amusementpark;

import com.amusementpark.model.FoodOwner;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for OwnerModal.fxml.
 *
 * Usage (from VendorController):
 *   ctrl.setMode(null)          → Add mode  (blank form)
 *   ctrl.setMode(existingOwner) → Edit mode (pre-filled)
 *   ctrl.setOnSave(owner -> …)  → called with the built FoodOwner on Save
 *   ctrl.setOnCancel(dialog::close)
 */
public class OwnerModalController implements Initializable {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label     errorLabel;

    /** Non-null when editing an existing owner. */
    private FoodOwner existing;

    private Consumer<FoodOwner> onSave;
    private Runnable            onCancel;

    // ── Initializable ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // nothing needed here; setMode() is called after load
    }

    // ── Public API called by VendorController ─────────────────────────────

    /**
     * @param owner {@code null} for Add mode, existing object for Edit mode.
     */
    public void setMode(FoodOwner owner) {
        this.existing = owner;
        if (owner != null) {
            firstNameField.setText(owner.getFirstName());
            lastNameField.setText(owner.getLastName());
            emailField.setText(owner.getEmail());
            phoneField.setText(owner.getPhone());
        }
    }

    public void setOnSave(Consumer<FoodOwner> callback)  { this.onSave   = callback; }
    public void setOnCancel(Runnable callback)            { this.onCancel = callback; }

    // ── Handlers ──────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideError();

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String email     = emailField.getText().trim();
        String phone     = phoneField.getText().trim();

        // ── Validation ────────────────────────────────────────────────────
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showError("First name and last name are required.");
            return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        // ── Build / update model ──────────────────────────────────────────
        FoodOwner result = (existing != null) ? existing : new FoodOwner();
        result.setFirstName(firstName);
        result.setLastName(lastName);
        result.setEmail(email.isEmpty() ? null : email);
        result.setPhone(phone.isEmpty()  ? null : phone);

        if (onSave != null) onSave.accept(result);
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) onCancel.run();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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

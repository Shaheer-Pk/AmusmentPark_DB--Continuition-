package com.amusementpark;

import com.amusementpark.model.BowlingBooking;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

public class BowlingModalController {

    @FXML private Label     modalTitle;
    @FXML private Label     modalSubtitle;
    @FXML private TextField laneField;
    @FXML private TextField amountField;
    @FXML private TextField timeField;
    @FXML private TextField cardIdField;
    @FXML private Label     validationLabel;
    @FXML private Button    saveButton;

    private Consumer<BowlingBooking> onSave;
    private Runnable                 onCancel;
    private BowlingBooking           existing;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void setMode(BowlingBooking b) {
        this.existing = b;
        if (b == null) {
            modalTitle.setText("Add Booking");
            modalSubtitle.setText("Fill in all fields below");
        } else {
            modalTitle.setText("Edit Booking");
            modalSubtitle.setText("Editing Booking #" + b.getBookingId());
            saveButton.setText("Save Changes");
            laneField.setText(String.valueOf(b.getLaneNumber()));
            amountField.setText(b.getAmount() != null ? b.getAmount().toPlainString() : "");
            timeField.setText(b.getTime() != null ? b.getTime().format(DT) : "");
            cardIdField.setText(String.valueOf(b.getCardId()));
        }
    }

    public void setOnSave(Consumer<BowlingBooking> cb) { this.onSave   = cb; }
    public void setOnCancel(Runnable cb)               { this.onCancel = cb; }

    @FXML private void handleSave() {
        validationLabel.setText("");
        String laneStr   = laneField.getText().trim();
        String amtStr    = amountField.getText().trim();
        String timeStr   = timeField.getText().trim();
        String cardStr   = cardIdField.getText().trim();

        if (laneStr.isEmpty() || amtStr.isEmpty() || timeStr.isEmpty() || cardStr.isEmpty()) {
            validationLabel.setText("All fields are required."); return;
        }

        int lane; BigDecimal amount; LocalDateTime time; int cardId;
        try { lane = Integer.parseInt(laneStr); if (lane < 1) throw new NumberFormatException(); }
        catch (NumberFormatException e) { validationLabel.setText("Lane must be a positive integer."); return; }

        try { amount = new BigDecimal(amtStr); if (amount.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { validationLabel.setText("Amount must be a valid positive number."); return; }

        try { time = LocalDateTime.parse(timeStr, DT); }
        catch (DateTimeParseException e) { validationLabel.setText("Date/time must be YYYY-MM-DD HH:MM format."); return; }

        try { cardId = Integer.parseInt(cardStr); }
        catch (NumberFormatException e) { validationLabel.setText("Card ID must be a valid integer."); return; }

        BowlingBooking result = existing != null ? existing : new BowlingBooking();
        result.setLaneNumber(lane);
        result.setAmount(amount);
        result.setTime(time);
        result.setCardId(cardId);

        if (onSave != null) onSave.accept(result);
    }

    @FXML private void handleCancel() { if (onCancel != null) onCancel.run(); }
}

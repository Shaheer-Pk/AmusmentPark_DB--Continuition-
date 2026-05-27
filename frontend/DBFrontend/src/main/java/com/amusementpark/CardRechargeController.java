package com.amusementpark;

import com.amusementpark.model.CustomerCard;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

public class CardRechargeController {

    @FXML private Label      modalTitle;
    @FXML private Label      modalSubtitle;
    @FXML private Label      currentBalanceLabel;
    @FXML private Label      currentPointsLabel;
    @FXML private TextField  topUpField;
    @FXML private TextField  addPointsField;
    @FXML private Label      validationLabel;
    @FXML private Button     rechargeButton;
    @FXML private Button     cancelButton;

    private Consumer<Number[]> onRecharge; // [cardId, topUp, addPoints]
    private Runnable           onCancel;
    private CustomerCard       card;

    private static final NumberFormat PKR = NumberFormat.getNumberInstance(Locale.US);
    static { PKR.setMinimumFractionDigits(2); PKR.setMaximumFractionDigits(2); }

    public void setCard(CustomerCard cc) {
        this.card = cc;
        modalTitle.setText("Recharge Card");
        modalSubtitle.setText(cc.getFullName() + "  •  Card #" + cc.getCardId());
        currentBalanceLabel.setText("PKR " + PKR.format(
            cc.getBalance() != null ? cc.getBalance() : BigDecimal.ZERO));
        currentPointsLabel.setText(
            cc.getPoints() != null ? cc.getPoints().toString() : "0");
    }

    public void setOnRecharge(Consumer<Number[]> cb) { this.onRecharge = cb; }
    public void setOnCancel(Runnable cb)             { this.onCancel   = cb; }

    @FXML
    private void handleRecharge() {
        validationLabel.setText("");
        String topUpStr = topUpField.getText().trim();
        String ptsStr   = addPointsField.getText().trim();

        if (topUpStr.isEmpty()) {
            validationLabel.setText("Top-up amount is required."); return;
        }

        BigDecimal topUp;
        try {
            topUp = new BigDecimal(topUpStr);
            if (topUp.compareTo(BigDecimal.ZERO) <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            validationLabel.setText("Top-up must be a positive number."); return;
        }

        int addPoints = 0;
        if (!ptsStr.isEmpty()) {
            try {
                addPoints = Integer.parseInt(ptsStr);
                if (addPoints < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                validationLabel.setText("Points must be a non-negative whole number."); return;
            }
        }

        if (onRecharge != null)
            onRecharge.accept(new Number[]{card.getCardId(), topUp, addPoints});
    }

    @FXML private void handleCancel() { if (onCancel != null) onCancel.run(); }
}

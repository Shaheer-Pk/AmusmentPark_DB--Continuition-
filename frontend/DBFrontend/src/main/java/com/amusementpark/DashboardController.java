package com.amusementpark;

import com.amusementpark.model.DashboardDAO;
import com.amusementpark.model.DashboardDAO.RevenueBreakdown;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label totalRevenueLabel;
    @FXML private Label activeRidesLabel;
    @FXML private Label staffCountLabel;
    @FXML private Label customerCountLabel;
    @FXML private Label rideRevenueLabel;
    @FXML private Label cinemaRevenueLabel;
    @FXML private Label bowlingRevenueLabel;
    @FXML private Label foodRevenueLabel;
    @FXML private Label foodStallCountLabel;

    private final DashboardDAO dashboardDAO = new DashboardDAO();

    // PKR formatting: "PKR 1,234,567.00"
    private static final NumberFormat PKR = NumberFormat.getNumberInstance(Locale.US);
    static {
        PKR.setMinimumFractionDigits(2);
        PKR.setMaximumFractionDigits(2);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshStats();
    }

    @FXML
    private void refreshStats() {
        // Set loading state immediately on FX thread
        totalRevenueLabel.setText("Loading…");
        activeRidesLabel.setText("—");
        staffCountLabel.setText("—");
        customerCountLabel.setText("—");
        foodStallCountLabel.setText("—");
        rideRevenueLabel.setText("—");
        cinemaRevenueLabel.setText("—");
        bowlingRevenueLabel.setText("—");
        foodRevenueLabel.setText("—");

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                return new DashboardData(
                    dashboardDAO.getRevenueBreakdown(),
                    dashboardDAO.getActiveRideCount(),
                    dashboardDAO.getTotalStaffCount(),
                    dashboardDAO.getTotalCustomerCount(),
                    dashboardDAO.getActiveFoodStallCount()
                );
            }
        };

        task.setOnSucceeded(e -> {
            DashboardData d = task.getValue();
            RevenueBreakdown rb2 = d.breakdown();

            Platform.runLater(() -> {
                totalRevenueLabel.setText("PKR " + PKR.format(rb2.total()));
                activeRidesLabel.setText(String.valueOf(d.activeRides()));
                staffCountLabel.setText(String.valueOf(d.staffCount()));
                customerCountLabel.setText(String.valueOf(d.customerCount()));
                foodStallCountLabel.setText(String.valueOf(d.foodStallCount()));

                rideRevenueLabel.setText("PKR " + PKR.format(rb2.rideRevenue()));
                cinemaRevenueLabel.setText("PKR " + PKR.format(rb2.cinemaRevenue()));
                bowlingRevenueLabel.setText("PKR " + PKR.format(rb2.bowlingRevenue()));
                foodRevenueLabel.setText("PKR " + PKR.format(rb2.foodRevenue()));
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            totalRevenueLabel.setText("Error");
            AlertHelper.showError(
                "Dashboard Error",
                "Failed to load stats: " + task.getException().getMessage()
            );
        }));

        new Thread(task, "dashboard-load-thread").start();
    }

    // ── Local record to bundle all DB results into one Task return ───────
    private record DashboardData(
        RevenueBreakdown breakdown,
        int activeRides,
        int staffCount,
        int customerCount,
        int foodStallCount
    ) {}
}

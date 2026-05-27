package com.amusementpark;

import com.amusementpark.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Button navDashboard;
    @FXML private Button navStaff;
    @FXML private Button navRides;
    @FXML private Button navBowling;
    @FXML private Button navCinema;
    @FXML private Button navCRM;
    @FXML private Button navVendors;

    @FXML private Label adminNameLabel;
    @FXML private Label adminEmailLabel;

    @FXML private StackPane contentArea;

    private List<Button> navButtons;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navButtons = List.of(navDashboard, navStaff, navRides, navBowling, navCinema, navCRM, navVendors);

        var admin = SessionManager.getInstance().getCurrentAdmin();
        if (admin != null) {
            adminNameLabel.setText(admin.getName());
            adminEmailLabel.setText(admin.getEmail());
        }

        showDashboard();
    }

    @FXML private void showDashboard() { loadView("/fxml/Dashboard.fxml",  navDashboard); }
    @FXML private void showStaff()     { loadView("/fxml/StaffView.fxml",   navStaff);    }
    @FXML private void showRides()     { loadView("/fxml/RidesView.fxml",   navRides);    }
    @FXML private void showBowling()   { loadView("/fxml/BowlingView.fxml", navBowling);  }
    @FXML private void showCinema()    { loadView("/fxml/CinemaView.fxml",  navCinema);   }
    @FXML private void showCRM()       { loadView("/fxml/CRMView.fxml",     navCRM);      }
    @FXML private void showVendors()   { loadView("/fxml/VendorView.fxml",  navVendors);  }

    @FXML
    private void handleLogout() {
        boolean confirmed = AlertHelper.showConfirm("Sign Out",
            "Are you sure you want to sign out?");
        if (!confirmed) return;

        SessionManager.getInstance().logout();

        try {
            URL loginUrl = getClass().getResource("/fxml/Login.fxml");
            URL cssUrl   = getClass().getResource("/css/style.css");
            FXMLLoader loader = new FXMLLoader(loginUrl);
            Scene scene = new Scene(loader.load(), 1280, 800);
            scene.getStylesheets().add(cssUrl.toExternalForm());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Apex Park — Admin Console");
        } catch (IOException ex) {
            AlertHelper.showError("Logout Error", "Failed to return to login: " + ex.getMessage());
        }
    }

    private void loadView(String fxmlPath, Button activeBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            setActiveNav(activeBtn);
        } catch (IOException ex) {
            AlertHelper.showError("Navigation Error",
                "Could not load: " + fxmlPath + "\n" + ex.getMessage());
        }
    }

    private void setActiveNav(Button active) {
        for (Button btn : navButtons) btn.getStyleClass().remove("nav-btn-active");
        if (!active.getStyleClass().contains("nav-btn-active"))
            active.getStyleClass().add("nav-btn-active");
    }
}

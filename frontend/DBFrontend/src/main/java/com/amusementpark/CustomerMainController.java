package com.amusementpark;

import com.amusementpark.model.UserAccount;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CustomerMainController implements Initializable {

    @FXML private Label customerNameLabel;
    @FXML private Label customerEmailLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UserAccount currentCustomer = SessionManager.getInstance().getCurrentUser();
        if (currentCustomer != null) {
            customerNameLabel.setText(currentCustomer.getFullName());
            customerEmailLabel.setText(currentCustomer.getEmail());
        }
    }

    @FXML
    private void handleLogout() {
        boolean confirmed = AlertHelper.showConfirm("Sign Out Confirmation", "Are you sure you want to exit your portal account session?");
        if (!confirmed) return;

        SessionManager.getInstance().logout();

        try {
            URL loginUrl = getClass().getResource("/fxml/Login.fxml");
            URL cssUrl   = getClass().getResource("/css/style.css");
            FXMLLoader loader = new FXMLLoader(loginUrl);
            Scene scene = new Scene(loader.load(), 1280, 800);
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            
            Stage stage = (Stage) customerNameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Apex Park — Workspace Access Portal");
        } catch (IOException ex) {
            AlertHelper.showError("Routing Redirection Failure", "Unable to load authorization workspace view panel context.");
        }
    }
}
package com.amusementpark;

import com.amusementpark.db.DatabaseConnection;
import com.amusementpark.navigation.NavigationService;
import com.amusementpark.navigation.ViewType;
import com.amusementpark.session.SessionManager;
import javafx.application.Application;
import javafx.stage.Stage;

// ─────────────────────────────────────────────────────────────────────────────
// MainApp  –  JavaFX entry point. Thin by design.
//
// RESPONSIBILITIES (exactly three):
//   1. Hand the Stage to NavigationService.
//   2. Load the first screen.
//   3. Clean up on exit.
//
// WHAT THIS CLASS DOES NOT DO:
//   No FXML paths.        Those live in ViewType.
//   No scene building.    That belongs to NavigationService.
//   No RBAC logic.        That belongs to SessionManager + controllers.
//   No navigation calls after start(). Controllers call NavigationService.
//
// HOW CONTROLLERS NAVIGATE:
//   NavigationService.navigateTo(ViewType.LOGIN);
//   SomeController ctrl = NavigationService.navigateToWithController(ViewType.STAFF_DASHBOARD);
// ─────────────────────────────────────────────────────────────────────────────
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        // 1. Give NavigationService ownership of the Stage.
        //    Every subsequent navigation goes through it — no controller
        //    ever holds a Stage reference directly.
        NavigationService.initialize(stage);

        // 2. First screen is always login.
        NavigationService.navigateTo(ViewType.LOGIN);
    }

    @Override
    public void stop() {
        // 3a. Wipe the session — no stale data if the app is relaunched.
        SessionManager.getInstance().clearSession();

        // 3b. Close the DB connection cleanly.
        DatabaseConnection.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
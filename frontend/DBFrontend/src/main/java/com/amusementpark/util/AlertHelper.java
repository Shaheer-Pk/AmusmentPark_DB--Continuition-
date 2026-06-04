package com.amusementpark.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

// ─────────────────────────────────────────────────────────────────────────────
// AlertHelper — centralised dialog utility.
//
// WHY THIS EXISTS:
//   Every controller needs to show errors, confirmations, and info dialogs.
//   Without this, each controller would repeat the same 6-line Alert block.
//   This keeps controllers clean — one line to show a dialog, no boilerplate.
//
// WHERE IT FITS IN THE FLOW:
//   DAO throws SQLException
//     → Controller catches it
//       → AlertHelper.showError() surfaces it to the user as a clean dialog
//       → e.printStackTrace() logs the full trace to console for the developer
//   User sees a readable message. Developer sees the full stack trace.
//   The app never silently fails or freezes.
//
// PACKAGE: com.amusementpark.util  (NOT session — this is a UI utility)
// ─────────────────────────────────────────────────────────────────────────────
public class AlertHelper {

    /**
     * Shows a blocking error dialog.
     * Use for: failed DB operations, invalid input, permission violations.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyStylesheet(alert);
        alert.showAndWait();
    }

    /**
     * Shows a blocking informational dialog.
     * Use for: successful operations ("Account created", "Ride added").
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyStylesheet(alert);
        alert.showAndWait();
    }

    /**
     * Shows a blocking confirmation dialog.
     * Returns true if the user clicked OK, false if they cancelled.
     * Use for: destructive actions ("Delete this ride?", "End this session?").
     */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyStylesheet(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Applies the global stylesheet to the alert dialog pane.
     * Without this, alerts render with default JavaFX styling that
     * clashes with the rest of the application's design.
     * Null-guarded — if the CSS file is missing the alert still shows.
     * The leading slash in /css* makes the path absolute from ROOT
     * Remove this leading slash and it tries to find css/style from the same dir alertHelper is in
     * (Without leading slash it fails)
     */
    private static void applyStylesheet(Alert alert) {
        var resource = AlertHelper.class.getResource("/css/style.css");         
        if (resource != null) {                                                      
            alert.getDialogPane().getStylesheets().add(resource.toExternalForm());
        }
    }
}
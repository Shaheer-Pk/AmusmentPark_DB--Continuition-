package com.amusementpark.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

// ─────────────────────────────────────────────────────────────────────────────
// NavigationService
//
// Owns the Stage and handles every scene and panel change in the application.
// No controller ever holds a Stage reference — they all go through here.
//
// TWO TYPES OF OPERATION:
//
//   navigateTo / navigateToWithController
//     Replaces the ENTIRE scene on the Stage window.
//     Old scene, old controllers, old nodes — all discarded.
//     Use only for: LOGIN → DASHBOARD, DASHBOARD → LOGIN (logout).
//
//   loadPanel / loadPanelWithController
//     Injects a module FXML into ONE container inside the dashboard.
//     The dashboard itself — sidebar, header, nav buttons — stays alive.
//     Only the content area in the centre changes.
//     Use for every in-dashboard navigation (Rides, Cinema, Finance, etc.)
//
// ZERO ACCESS CONTROL HERE.
//   Whether a navigation is permitted is decided by the calling controller
//   via SessionManager.hasPermission(). This class only executes the
//   mechanical action of loading and displaying scenes/panels.
//
// FAIL LOUDLY.
//   A missing FXML file throws RuntimeException with a descriptive message.
//   Silent failures (returning null, printing to stderr) hide bugs.
// ─────────────────────────────────────────────────────────────────────────────
public class NavigationService {

    // The one and only Stage (window) of the application.
    // Handed to us by MainApp.start() via initialize().
    // Private so nothing else can reach past NavigationService to touch it.
    private static Stage primaryStage;

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Stores the application's primary Stage.
     * Must be called ONCE from MainApp.start() before anything else.
     * Every public method here calls guardInitialised() which will throw
     * clearly if this was skipped.
     */
    public static void initialize(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("Stage cannot be null.");
        primaryStage = stage;
    }

    // ── Full-screen Navigation ────────────────────────────────────────────────

    /**
     * Loads an FXML file and replaces the entire scene on the Stage.
     *
     * When to use:
     *   - Transitioning from LOGIN screen to DASHBOARD after successful login.
     *   - Transitioning from DASHBOARD back to LOGIN on logout.
     *
     * When NOT to use:
     *   - Switching between modules inside the dashboard (use loadPanel instead).
     *   - A full scene swap destroys the dashboard controller. If you call this
     *     for a rides → cinema switch, the sidebar rebuilds from scratch every
     *     single time. That is wasteful and visually jarring.
     */
    public static void navigateTo(ViewType view) {
        guardInitialised();
        try {
            FXMLLoader loader = buildLoader(view);
            Parent root = loader.load();
            applyScene(root, view);
        } catch (IOException e) {
            throw new RuntimeException(
                "NavigationService: Failed to load " + view.getFxmlPath()
                + " — " + e.getMessage(), e);
        }
    }

    /**
     * Same as navigateTo() but returns the destination controller.
     *
     * Use this when the calling controller needs to pass data into the
     * destination BEFORE it becomes visible on screen.
     *
     * Example — AuthController after login:
     *   DashboardController ctrl =
     *       NavigationService.navigateToWithController(ViewType.DASHBOARD);
     *   ctrl.initSession(user);   // hand over the logged-in user before render
     *
     * If you don't need to talk to the destination controller, use navigateTo().
     */
    public static <T> T navigateToWithController(ViewType view) {
        guardInitialised();
        try {
            FXMLLoader loader = buildLoader(view);
            Parent root = loader.load();
            applyScene(root, view);
            return loader.getController();      // Returns the controller marked in the fxml file as fx:controller= "Controller name"
        } catch (IOException e) {
            throw new RuntimeException(
                "NavigationService: Failed to load " + view.getFxmlPath()
                + " — " + e.getMessage(), e);
        }
    }

    // ── Panel Injection ───────────────────────────────────────────────────────

    /**
     * Loads a module FXML and injects it into the dashboard content area.
     * The dashboard sidebar and header are completely untouched.
     *
     * Use for every in-dashboard navigation:
     *   NavigationService.loadPanel(ViewType.PANEL_RIDES, contentArea);
     *   NavigationService.loadPanel(ViewType.PANEL_FINANCE, contentArea);
     *
     * The panel's own controller.initialize() runs automatically after load.
     * That controller reads SessionManager itself to show/hide its own buttons.
     * You do not need to do anything after this call for standard panels.
     *
     * @param view       the panel to load (must be a PANEL_* ViewType entry)
     * @param container  the AnchorPane in dashboard.fxml that holds the content area
     */
    public static void loadPanel(ViewType view, Pane container) {
        // We don't need the controller back — delegate to internal and discard.
        loadPanelInternal(view, container);
    }

    /**
     * Same as loadPanel() but returns the panel's controller after load.
     *
     * Use for master-detail patterns where the calling controller needs to
     * tell the newly loaded panel WHICH item to display.
     *
     * Example — user clicks a row in the staff table:
     *   StaffDetailController ctrl =
     *       NavigationService.loadPanelWithController(ViewType.PANEL_STAFF_DETAIL, contentArea);
     *   ctrl.loadStaff(selectedStaffId);
     *
     * Without this, your only alternatives would be:
     *   (a) Query the database AGAIN in initialize() — wasteful and fragile.
     *   (b) Store the selected ID in a static variable — creates invisible
     *       coupling between controllers that will bite you later.
     *
     * If you don't need to talk to the panel's controller after loading,
     * use loadPanel() — simpler and clearer at the call site.
     *
     * @param view       the panel to load
     * @param container  the AnchorPane content area in dashboard.fxml
     * @param <T>        the controller type — inferred from the assignment
     * @return           the panel's controller, already initialised
     */
    public static <T> T loadPanelWithController(ViewType view, Pane container) {
        return loadPanelInternal(view, container);
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    /**
     * The actual panel loading logic shared by both loadPanel() and
     * loadPanelWithController(). Both public methods exist only to give
     * callers a clear, intention-revealing API. Internally they both
     * do the same thing — this method.
     *
     * WHAT HAPPENS HERE, STEP BY STEP:
     *
     * 1. buildLoader(view)
     *    Validates the FXML path and creates an FXMLLoader pointed at that file.
     *
     * 2. loader.load()
     *    JavaFX reads the FXML XML, constructs every node described in it
     *    (VBox, Button, TableView, etc.) as real Java objects in memory,
     *    instantiates the controller class declared in the FXML's fx:controller
     *    attribute, injects all @FXML-annotated fields into that controller,
     *    then calls that controller's initialize() method.
     *    The return value is the ROOT NODE of the panel — the top-level
     *    container that holds everything else in that FXML file.
     *
     * 3. container.getChildren().setAll(panel)
     *    'container' is the AnchorPane sitting in the centre of the dashboard.
     *    getChildren() returns the list of everything currently displayed inside it.
     *    setAll() atomically replaces that entire list with just the new panel.
     *    The old panel and all its child nodes are removed in one operation
     *    and become eligible for garbage collection.
     *    Result: the content area now shows the new panel.
     *
     * 4. AnchorPane anchor pinning
     *    AnchorPane positions children by setting a fixed distance between
     *    the child's edge and the container's corresponding edge.
     *    Setting ALL FOUR anchors to 0.0 means:
     *      - Panel's top edge   = 0px from container's top edge
     *      - Panel's bottom edge = 0px from container's bottom edge
     *      - Panel's left edge  = 0px from container's left edge
     *      - Panel's right edge = 0px from container's right edge
     *    Effect: the panel is STRETCHED to fill every pixel of the container.
     *    Without this the panel would sit at its preferred/natural size
     *    in the top-left corner — leaving empty space on the right and bottom.
     *    We only do this when the container IS an AnchorPane — other Pane
     *    types have their own sizing rules and don't use anchor properties.
     *
     * 5. loader.getController()
     *    After load() completes, the controller object lives inside the loader.
     *    getController() hands it back to us.
     *    The (T) cast is unchecked — the caller determines the type via
     *    their assignment. If they assign the wrong controller type it will
     *    throw a ClassCastException at runtime, not compile time.
     *    This is the standard JavaFX pattern for retrieving controllers.
     *
     * @param view       the panel ViewType to load
     * @param container  the Pane (typically AnchorPane) to inject into
     * @param <T>        the controller type expected by the caller
     * @return           the panel's controller instance, already initialised
     */
    @SuppressWarnings("unchecked")
    private static <T> T loadPanelInternal(ViewType view, Pane container) {
        try {
            // Step 1: Validate path and create the loader.
            FXMLLoader loader = buildLoader(view);

            // Step 2: Parse the FXML, build the node tree, run initialize().
            Parent panel = loader.load();

            // Step 3: Swap the content area — remove old panel, insert new one.
            container.getChildren().setAll(panel);

            // Step 4: If the container is an AnchorPane, pin all four edges
            // so the panel stretches to fill the entire content area.
            if (container instanceof AnchorPane) {
                AnchorPane.setTopAnchor(panel, 0.0);
                AnchorPane.setBottomAnchor(panel, 0.0);
                AnchorPane.setLeftAnchor(panel, 0.0);
                AnchorPane.setRightAnchor(panel, 0.0);
            }

            // Step 5: Return the controller for callers who need it.
            // loadPanel() calls this and discards the return value.
            // loadPanelWithController() calls this and passes it back to the caller.
            return (T) loader.getController();

        } catch (IOException e) {
            throw new RuntimeException(
                "NavigationService: Failed to load panel " + view.getFxmlPath()
                + " — " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the FXML file exists at the path stored in the ViewType
     * and returns a configured FXMLLoader pointed at it.
     *
     * WHY THIS EXISTS:
     *   Without the null check on the URL, a missing FXML file produces a
     *   NullPointerException deep inside FXMLLoader with no message about
     *   which file was missing or why. This check catches the problem at
     *   the source and produces a descriptive error immediately.
     *
     *   Four methods need a loader (navigateTo, navigateToWithController,
     *   loadPanel, loadPanelWithController). This centralises the check
     *   instead of duplicating it in each method.
     */
    private static FXMLLoader buildLoader(ViewType view) {
        URL url = NavigationService.class.getResource(view.getFxmlPath());
        if (url == null) {
            throw new RuntimeException(
                "NavigationService: FXML not found → " + view.getFxmlPath()
                + "\nCheck that the file exists under src/main/resources.");
        }
        return new FXMLLoader(url);
    }

    /**
     * Applies a loaded scene root to the Stage — creates a new Scene on first
     * call, reuses and swaps the root on subsequent calls.
     *
     * WHY REUSE THE SCENE:
     *   Creating a new Scene object on every navigation means re-adding the
     *   stylesheet every time, re-setting the stage, re-rendering from scratch.
     *   Reusing the Scene and calling setRoot() is cheaper — JavaFX only
     *   re-renders what changed.
     *
     * WHY THIS IS A SEPARATE METHOD:
     *   Both navigateTo() and navigateToWithController() need these same
     *   five operations. Extracting them here avoids duplicating five lines
     *   in two places that must always stay in sync.
     */
    private static void applyScene(Parent root, ViewType view) {
        Scene current = primaryStage.getScene();

        if (current == null) {
            // First ever scene — create it with fixed window dimensions.
            current = new Scene(root, 1280, 800);
            primaryStage.setScene(current);
        } else {
            // Scene already exists — swap only the root node.
            // Stylesheet stays applied, Stage stays configured.
            current.setRoot(root);
        }

        // Apply the global stylesheet if it isn't already on this Scene.
        // The contains() check prevents duplicate entries on repeated navigations.
        URL cssUrl = NavigationService.class.getResource("/css/style.css");
        if (cssUrl != null && !current.getStylesheets().contains(cssUrl.toExternalForm())) {
            current.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle(view.getWindowTitle());
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Throws immediately if NavigationService.initialize() was never called.
     *
     * WHY THIS EXISTS:
     *   If primaryStage is null and a controller calls navigateTo(), the
     *   NullPointerException would come from somewhere inside JavaFX internals
     *   with no message pointing back to the real cause.
     *   This guard catches the mistake at the entry point of every public
     *   method and tells you exactly what went wrong and how to fix it.
     *   One call at the top of each public method instead of a null check
     *   duplicated across four methods.
     */
    private static void guardInitialised() {
        if (primaryStage == null) {
            throw new IllegalStateException(
                "NavigationService has not been initialised. "
                + "Call NavigationService.initialize(stage) from MainApp.start() first.");
        }
    }
}
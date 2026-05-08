package com.coffeescheduler.ui;

import com.coffeescheduler.generator.GeneratorResult;
import com.coffeescheduler.generator.ScheduleScorer;
import com.coffeescheduler.generator.TwoPhaseGenerator;
import com.coffeescheduler.io.ExcelExporter;
import com.coffeescheduler.io.ScheduleJson;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.RuleViolation;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.Selection;
import com.coffeescheduler.model.WeekState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MainWindow extends BorderPane {

    private final ObjectProperty<Selection> selection = new SimpleObjectProperty<>(Selection.NONE);
    private final Label statusFile = new Label("Untitled");
    private final Label statusSummary = new Label("");
    private final Label statusViolations = new Label("0 violations");
    private final RosterPanel rosterPanel = new RosterPanel(this::onRosterChanged, this::onClinicianSelected);
    private final RulesPanel rulesPanel = new RulesPanel(this::onRulesChanged);
    private final VBox leftPane = new VBox(rosterPanel, rulesPanel);
    private final ViolationsPanel violationsPanel = new ViolationsPanel(this::jumpToViolation);
    private DetailsPanel detailsPanel;
    private Schedule schedule;
    private File currentFile;
    private boolean dirty;
    private boolean rosterVisible = true;
    private boolean detailsVisible = true;

    public MainWindow() {
        setTop(buildTopBar());
        VBox.setVgrow(rosterPanel, Priority.ALWAYS);
        setLeft(leftPane);
        violationsPanel.setVisible(false);
        violationsPanel.setManaged(false);
        setBottom(new VBox(violationsPanel, buildStatusBar()));
        showEmptyState();
    }

    private void loadSchedule(Schedule newSchedule) {
        this.schedule = newSchedule;
        selection.set(Selection.NONE);
        rosterPanel.setSchedule(schedule);
        rulesPanel.setSchedule(schedule);
        rebuildGrid();
        statusViolations.setText("0 violations");
        violationsPanel.setViolations(List.of());
        violationsPanel.setVisible(false);
        violationsPanel.setManaged(false);
        setDirty(false);
    }

    private void onRosterChanged() {
        rulesPanel.refresh();
        rebuildGrid();
        setDirty(true);
    }

    private void onRulesChanged() {
        setDirty(true);
    }

    private void onClinicianSelected(Clinician c) {
        selection.set(new Selection.OfClinician(c));
    }

    private void onClinicianEdited(Clinician updated) {
        rebuildGrid();
        selection.set(new Selection.OfClinician(updated));
        setDirty(true);
    }

    private void onScheduleEdited(DetailsPanel details) {
        details.refresh();
        rosterPanel.refresh();
        setDirty(true);
    }

    private void rebuildGrid() {
        if (schedule == null) return;
        selection.set(Selection.NONE);
        detailsPanel = new DetailsPanel(schedule, selection, this::onClinicianEdited);
        setCenter(new ScheduleGrid(schedule, selection, () -> onScheduleEdited(detailsPanel)));
        setRight(detailsVisible ? detailsPanel : null);
        statusSummary.setText(ScheduleSummary.format(schedule));
        rosterPanel.refresh();
    }

    private void setDirty(boolean value) {
        this.dirty = value;
        updateTitle();
        statusFile.setText(currentFile != null ? currentFile.getName() : "Untitled");
    }

    private void updateTitle() {
        String title = "Coffee Scheduler";
        if (currentFile != null) {
            title = currentFile.getName() + " — " + title;
        }
        if (dirty) {
            title = "*" + title;
        }
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            stage.setTitle(title);
        }
    }

    private void save() {
        if (schedule == null) return;
        if (currentFile == null) {
            saveAs();
        } else {
            writeFile(currentFile);
        }
    }

    private void saveAs() {
        if (schedule == null) return;
        FileChooser chooser = jsonFileChooser("Save Schedule");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            if (!file.getName().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            writeFile(file);
        }
    }

    private void writeFile(File file) {
        try {
            Files.writeString(file.toPath(), ScheduleJson.toJson(schedule));
            currentFile = file;
            setDirty(false);
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage()).showAndWait();
        }
    }

    private void open() {
        FileChooser chooser = jsonFileChooser("Open Schedule");
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                Schedule loaded = ScheduleJson.fromJson(json);
                currentFile = file;
                loadSchedule(loaded);
            } catch (IOException | IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to open: " + ex.getMessage()).showAndWait();
            }
        }
    }

    private void editSettings() {
        if (schedule == null) return;
        new ScheduleSettingsDialog(schedule).showAndWait().ifPresent(applied -> {
            rebuildGrid();
            setDirty(true);
        });
    }

    private void generateSchedule() {
        if (schedule == null || schedule.roster().isEmpty()) return;

        ButtonType overwrite = new ButtonType("Overwrite everything");
        ButtonType keepEdits = new ButtonType("Keep my edits as fixed");
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, null, overwrite, keepEdits, ButtonType.CANCEL);
        dialog.setTitle("Generate Schedule");
        dialog.setHeaderText("How should the generator handle existing assignments?");
        ButtonType choice = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == ButtonType.CANCEL) return;

        if (choice == overwrite) {
            for (Clinician c : schedule.roster()) {
                for (int w = 1; w <= schedule.lengthWeeks(); w++) {
                    if (schedule.stateOf(c, w) == WeekState.ON) {
                        schedule.setState(c, w, null);
                    }
                }
            }
            schedule.clearAllPins();
        } else {
            for (Clinician c : schedule.roster()) {
                for (int w = 1; w <= schedule.lengthWeeks(); w++) {
                    if (schedule.stateOf(c, w) == WeekState.ON && !schedule.isPinned(c, w)) {
                        schedule.setState(c, w, null);
                    }
                }
            }
        }

        GeneratorResult result = new TwoPhaseGenerator().generate(schedule);
        int count = result.violations().size();
        statusViolations.setText(count == 0 ? "0 violations" : count + " violations ▴");
        violationsPanel.setViolations(result.violations());
        if (count > 0) {
            violationsPanel.setVisible(true);
            violationsPanel.setManaged(true);
        }
        rebuildGrid();
        if (getCenter() instanceof ScheduleGrid grid) {
            grid.setViolations(buildViolationMap(result.violations()));
        }
        setDirty(true);
    }

    private void checkViolations() {
        if (schedule == null || schedule.roster().isEmpty()) return;
        ScheduleScorer.ScoreResult result = new ScheduleScorer().score(schedule);
        List<RuleViolation> violations = result.violations();
        int count = violations.size();
        statusViolations.setText(count == 0 ? "0 violations" : count + " violations ▴");
        violationsPanel.setViolations(violations);
        violationsPanel.setVisible(count > 0);
        violationsPanel.setManaged(count > 0);
        if (getCenter() instanceof ScheduleGrid grid) {
            grid.setViolations(buildViolationMap(violations));
        }
    }

    private void exportExcel() {
        if (schedule == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to Excel");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            if (!file.getName().endsWith(".xlsx")) {
                file = new File(file.getParentFile(), file.getName() + ".xlsx");
            }
            try {
                ExcelExporter.export(schedule, file.toPath());
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to export: " + ex.getMessage()).showAndWait();
            }
        }
    }

    private void clearAllAssignments() {
        if (schedule == null) return;
        for (Clinician c : schedule.roster()) {
            for (int w = 1; w <= schedule.lengthWeeks(); w++) {
                schedule.setState(c, w, null);
            }
        }
        statusViolations.setText("0 violations");
        rebuildGrid();
        setDirty(true);
    }

    void requestClose() {
        if (dirty) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "You have unsaved changes. Close without saving?",
                    javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText(null);
            if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                    != javafx.scene.control.ButtonType.OK) {
                return;
            }
        }
        getScene().getWindow().hide();
    }

    private static FileChooser jsonFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Schedule files (*.json)", "*.json"));
        return chooser;
    }

    private void showEmptyState() {
        Label prompt = new Label("No schedule open. Use File → New (Ctrl+N) to create one.");
        prompt.setStyle("-fx-text-fill: #888; -fx-font-size: 14;");
        VBox box = new VBox(prompt);
        box.setAlignment(Pos.CENTER);
        setCenter(box);
        setRight(null);
        statusSummary.setText("");
    }

    private VBox buildTopBar() {
        return new VBox(buildMenuBar(), buildToolBar());
    }

    private MenuBar buildMenuBar() {
        MenuItem newItem = menuItem("New", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        newItem.setOnAction(e -> NewScheduleDialog.prompt().ifPresent(s -> {
            currentFile = null;
            loadSchedule(s);
        }));

        MenuItem openItem = menuItem("Open…", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> open());

        MenuItem saveItem = menuItem("Save", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        saveItem.setOnAction(e -> save());

        MenuItem saveAsItem = menuItem("Save As…", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> saveAs());

        MenuItem exportItem = menuItem("Export to Excel…", new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        exportItem.setOnAction(e -> exportExcel());

        MenuItem exitItem = menuItem("Exit", new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        exitItem.setOnAction(e -> requestClose());

        Menu file = new Menu("File");
        file.getItems().addAll(
                newItem, openItem, saveItem, saveAsItem,
                new SeparatorMenuItem(),
                exportItem,
                new SeparatorMenuItem(),
                exitItem);

        Menu edit = new Menu("Edit");
        edit.getItems().addAll(
                menuItem("Cut", null),
                menuItem("Copy", null),
                menuItem("Paste", null),
                new SeparatorMenuItem(),
                menuItem("Find clinician…", new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN)));

        MenuItem settingsItem = menuItem("Settings…", null);
        settingsItem.setOnAction(e -> editSettings());

        MenuItem generateItem = menuItem("Generate", new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN));
        generateItem.setOnAction(e -> generateSchedule());

        MenuItem checkItem = menuItem("Check", new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        checkItem.setOnAction(e -> checkViolations());

        MenuItem clearItem = menuItem("Clear all assignments", null);
        clearItem.setOnAction(e -> clearAllAssignments());

        Menu schedule = new Menu("Schedule");
        schedule.getItems().addAll(
                settingsItem,
                generateItem,
                checkItem,
                clearItem);

        MenuItem toggleViolations = menuItem("Toggle violations panel", null);
        toggleViolations.setOnAction(e -> toggleViolationsPanel());

        MenuItem toggleRoster = menuItem("Toggle roster panel", null);
        toggleRoster.setOnAction(e -> toggleRosterPanel());

        MenuItem toggleDetails = menuItem("Toggle details panel", null);
        toggleDetails.setOnAction(e -> toggleDetailsPanel());

        Menu view = new Menu("View");
        view.getItems().addAll(
                toggleRoster,
                toggleDetails,
                toggleViolations);

        Menu help = new Menu("Help");
        help.getItems().add(menuItem("About", null));

        return new MenuBar(file, edit, schedule, view, help);
    }

    private MenuItem menuItem(String label, KeyCombination accelerator) {
        MenuItem item = new MenuItem(label);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        return item;
    }

    private ToolBar buildToolBar() {
        Button genBtn = new Button("Generate");
        genBtn.setOnAction(e -> generateSchedule());
        Button checkBtn = new Button("Check");
        checkBtn.setOnAction(e -> checkViolations());
        return new ToolBar(genBtn, checkBtn);
    }

    private void toggleRosterPanel() {
        rosterVisible = !rosterVisible;
        setLeft(rosterVisible ? leftPane : null);
    }

    private void toggleDetailsPanel() {
        detailsVisible = !detailsVisible;
        setRight(detailsVisible && detailsPanel != null ? detailsPanel : null);
    }

    private void toggleViolationsPanel() {
        boolean show = !violationsPanel.isVisible();
        violationsPanel.setVisible(show);
        violationsPanel.setManaged(show);
    }

    private static Map<Selection.CellRef, List<RuleViolation>> buildViolationMap(List<RuleViolation> violations) {
        Map<Selection.CellRef, List<RuleViolation>> map = new HashMap<>();
        for (RuleViolation v : violations) {
            if (v.clinician() != null && v.week() != null) {
                Selection.CellRef ref = new Selection.CellRef(v.clinician(), v.week());
                map.computeIfAbsent(ref, k -> new ArrayList<>()).add(v);
            }
        }
        return map;
    }

    private void jumpToViolation(RuleViolation v) {
        if (schedule == null) return;
        if (v.clinician() != null && v.week() != null) {
            selection.set(new Selection.OfCells(
                    java.util.List.of(new Selection.CellRef(v.clinician(), v.week()))));
        } else if (v.clinician() != null) {
            selection.set(new Selection.OfClinician(v.clinician()));
        } else if (v.week() != null) {
            selection.set(new Selection.OfWeek(v.week()));
        }
    }

    private HBox buildStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusViolations.setCursor(javafx.scene.Cursor.HAND);
        statusViolations.setOnMouseClicked(e -> toggleViolationsPanel());

        HBox bar = new HBox(12, statusFile, statusSummary, spacer, statusViolations);
        bar.setPadding(new Insets(4, 8, 4, 8));
        return bar;
    }
}

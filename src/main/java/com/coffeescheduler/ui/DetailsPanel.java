package com.coffeescheduler.ui;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.DemandOverride;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.Selection;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
import com.coffeescheduler.model.WeeklyDemand;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Consumer;

public class DetailsPanel extends TitledPane {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(UIConstants.DATE_PATTERN_DETAIL);

    private final Schedule schedule;
    private final ObjectProperty<Selection> selection;
    private final Consumer<Clinician> onClinicianEdited;
    private final Runnable onDemandChanged;
    private final VBox body = new VBox(UIConstants.PANEL_SPACING);

    public DetailsPanel(Schedule schedule, ObjectProperty<Selection> selection,
                        Consumer<Clinician> onClinicianEdited, Runnable onDemandChanged) {
        super("Details", null);
        this.schedule = schedule;
        this.selection = selection;
        this.onClinicianEdited = onClinicianEdited;
        this.onDemandChanged = onDemandChanged;
        body.setPadding(UIConstants.PANEL_INSETS);
        setContent(body);
        setCollapsible(true);
        setPrefWidth(UIConstants.DETAILS_PANEL_WIDTH);
        selection.addListener((obs, oldSel, newSel) -> render(newSel));
        render(selection.get());
    }

    public void refresh() {
        render(selection.get());
    }

    private void render(Selection sel) {
        body.getChildren().clear();
        switch (sel) {
            case Selection.None _ -> renderNone();
            case Selection.OfCells c when c.cells().size() == 1 ->
                    renderCell(c.cells().getFirst().clinician(), c.cells().getFirst().week());
            case Selection.OfCells c -> renderMultiCell(c);
            case Selection.OfWeek w -> renderWeek(w.week());
            case Selection.OfClinician c -> renderClinician(c.clinician());
        }
    }

    private void renderNone() {
        body.getChildren().addAll(
                heading("Schedule"),
                new Label(ScheduleSummary.format(schedule)),
                muted("Click a cell, week, or clinician for details."));
    }

    private void renderMultiCell(Selection.OfCells cells) {
        long onCount = cells.cells().stream()
                .filter(r -> schedule.stateOf(r.clinician(), r.week()) == WeekState.ON).count();
        long unavCount = cells.cells().stream()
                .filter(r -> schedule.stateOf(r.clinician(), r.week()) == WeekState.UNAVAILABLE).count();
        long offCount = cells.cells().size() - onCount - unavCount;
        body.getChildren().addAll(
                heading("Selection"),
                kv("Cells", String.valueOf(cells.cells().size())),
                kv("On", String.valueOf(onCount)),
                kv("Unavailable", String.valueOf(unavCount)),
                kv("Off", String.valueOf(offCount)),
                muted("Press O / U / Space to set state on all."));
    }

    private void renderCell(Clinician clinician, int week) {
        WeekState state = schedule.stateOf(clinician, week);
        Set<WeekMarker> markers = schedule.markersOf(clinician, week);
        body.getChildren().addAll(
                heading("Cell"),
                kv("Clinician", clinician.name()),
                kv("Week", WeekHeader.format(week, schedule.startMonday(), schedule.lengthWeeks(), schedule.scheduleBlockOf(week))),
                kv("Date", schedule.startMonday().plusWeeks(week - 1).format(DATE)),
                kv("State", state == null ? "off" : state.name().toLowerCase()),
                kv("Markers", markers.isEmpty() ? "—" : markers.toString()));
    }

    private void renderWeek(int week) {
        Set<Clinician> on = schedule.onClinicians(week);
        VBox onList = new VBox(2);
        if (on.isEmpty()) {
            onList.getChildren().add(muted("(none)"));
        } else {
            for (Clinician c : on) {
                onList.getChildren().add(new Label("• " + c.name()));
            }
        }

        WeeklyDemand effective = schedule.demandFor(week);
        DemandOverride existing = findOverrideContaining(week);

        body.getChildren().addAll(
                heading("Week"),
                kv("Index", "Week " + week),
                kv("Date", schedule.startMonday().plusWeeks(week - 1).format(DATE)),
                kv("Coverage", on.size() + " on clinic"),
                kv("Demand", effective.min() + " / " + effective.ideal() + " / " + effective.max()
                        + (existing == null ? " (default)" : " (override)")),
                new Label("On this week:"),
                onList);

        if (existing == null) {
            Button overrideBtn = new Button("Override demand...");
            overrideBtn.setOnAction(e -> showOverrideEditor(week, null));
            body.getChildren().add(overrideBtn);
        } else {
            showOverrideEditor(week, existing);
        }
    }

    private void showOverrideEditor(int week, DemandOverride existing) {
        WeeklyDemand d = existing != null ? existing.demand() : schedule.defaultDemand();
        int startInit = existing != null ? existing.startWeek() : week;
        int endInit = existing != null ? existing.endWeek() : week;

        Spinner<Integer> startSpin = intSpinner(1, schedule.lengthWeeks(), startInit);
        Spinner<Integer> endSpin = intSpinner(1, schedule.lengthWeeks(), endInit);
        Spinner<Integer> minSpin = intSpinner(0, 20, d.min());
        Spinner<Integer> idealSpin = intSpinner(0, 20, d.ideal());
        Spinner<Integer> maxSpin = intSpinner(0, 20, d.max());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(UIConstants.DETAIL_GRID_HGAP);
        grid.setVgap(UIConstants.DETAIL_GRID_VGAP);
        int row = 0;
        grid.add(new Label("Start week:"), 0, row);
        grid.add(startSpin, 1, row++);
        grid.add(new Label("End week:"), 0, row);
        grid.add(endSpin, 1, row++);
        grid.add(new Label("Min:"), 0, row);
        grid.add(minSpin, 1, row++);
        grid.add(new Label("Ideal:"), 0, row);
        grid.add(idealSpin, 1, row++);
        grid.add(new Label("Max:"), 0, row);
        grid.add(maxSpin, 1, row);

        Button apply = new Button("Apply");
        Runnable validate = () -> {
            String err = validateOverride(startSpin.getValue(), endSpin.getValue(),
                    minSpin.getValue(), idealSpin.getValue(), maxSpin.getValue(),
                    existing);
            errorLabel.setText(err != null ? err : "");
            apply.setDisable(err != null);
        };
        startSpin.valueProperty().addListener((o, a, b) -> validate.run());
        endSpin.valueProperty().addListener((o, a, b) -> validate.run());
        minSpin.valueProperty().addListener((o, a, b) -> validate.run());
        idealSpin.valueProperty().addListener((o, a, b) -> validate.run());
        maxSpin.valueProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        apply.setOnAction(e -> {
            DemandOverride replacement = new DemandOverride(
                    startSpin.getValue(), endSpin.getValue(),
                    new WeeklyDemand(minSpin.getValue(), idealSpin.getValue(), maxSpin.getValue()));
            if (existing != null) {
                schedule.replaceDemandOverride(existing.startWeek(), replacement);
            } else {
                schedule.addDemandOverride(replacement);
            }
            onDemandChanged.run();
            render(selection.get());
        });

        body.getChildren().addAll(heading("Demand Override"), grid, errorLabel, apply);

        if (existing != null) {
            Button remove = new Button("Remove");
            remove.setOnAction(e -> {
                schedule.removeDemandOverride(existing.startWeek());
                onDemandChanged.run();
                render(selection.get());
            });
            body.getChildren().add(remove);
        }
    }

    private String validateOverride(int start, int end, int min, int ideal, int max,
                                    DemandOverride existing) {
        if (end < start) return "End week must be >= start week.";
        if (end > schedule.lengthWeeks()) return "End week exceeds schedule length.";
        if (min > ideal) return "Min must be <= ideal.";
        if (ideal > max) return "Ideal must be <= max.";
        for (DemandOverride o : schedule.demandOverrides()) {
            if (existing != null && o.startWeek() == existing.startWeek()) continue;
            if (start <= o.endWeek() && end >= o.startWeek()) {
                return "Overlaps with existing override (weeks " + o.startWeek() + "-" + o.endWeek() + ").";
            }
        }
        return null;
    }

    private DemandOverride findOverrideContaining(int week) {
        for (DemandOverride o : schedule.demandOverrides()) {
            if (week >= o.startWeek() && week <= o.endWeek()) return o;
        }
        return null;
    }

    private void renderClinician(Clinician clinician) {
        ContractedWeeks cw = clinician.contractedWeeks();
        BlockLengthRange pref = clinician.preferredBlockLength();

        TextField nameField = new TextField(clinician.name());
        Spinner<Integer> contractMin = intSpinner(0, 52, cw.min());
        Spinner<Integer> contractMax = intSpinner(0, 52, cw.max());
        Spinner<Integer> maxBlock = intSpinner(2, 26, clinician.maxBlockLength());
        Spinner<Integer> maxBlocksAtMax = intSpinner(0, 26, clinician.maxBlocksAtMaxLength());
        Spinner<Integer> prefMin = intSpinner(2, 26, pref.min());
        Spinner<Integer> prefMax = intSpinner(2, 26, pref.max());

        GridPane grid = new GridPane();
        grid.setHgap(UIConstants.DETAIL_GRID_HGAP);
        grid.setVgap(UIConstants.DETAIL_GRID_VGAP);
        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Contracted min:"), 0, row);
        grid.add(contractMin, 1, row++);
        grid.add(new Label("Contracted max:"), 0, row);
        grid.add(contractMax, 1, row++);
        grid.add(new Label("Max block length:"), 0, row);
        grid.add(maxBlock, 1, row++);
        grid.add(new Label("Max blocks at max:"), 0, row);
        grid.add(maxBlocksAtMax, 1, row++);
        grid.add(new Label("Preferred block min:"), 0, row);
        grid.add(prefMin, 1, row++);
        grid.add(new Label("Preferred block max:"), 0, row);
        grid.add(prefMax, 1, row);

        Button apply = new Button("Apply");
        apply.setOnAction(e -> {
            String name = nameField.getText().strip();
            if (name.isBlank()) return;
            Clinician updated = new Clinician(
                    name,
                    new ContractedWeeks(contractMin.getValue(), contractMax.getValue()),
                    maxBlock.getValue(),
                    maxBlocksAtMax.getValue(),
                    new BlockLengthRange(prefMin.getValue(), prefMax.getValue()));
            schedule.replaceClinician(clinician, updated);
            selection.set(new Selection.OfClinician(updated));
            onClinicianEdited.accept(updated);
        });

        body.getChildren().addAll(heading("Clinician"), grid, apply);
    }

    private static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> s = new Spinner<>();
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        s.setEditable(true);
        s.setPrefWidth(UIConstants.SPINNER_WIDTH);
        return s;
    }

    private static Label heading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: " + (int) UIConstants.FONT_SIZE + ";");
        return l;
    }

    private static Label kv(String key, String value) {
        Label l = new Label(key + ": " + value);
        l.setWrapText(true);
        return l;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + UIConstants.COLOR_MUTED_TEXT + ";");
        return l;
    }
}

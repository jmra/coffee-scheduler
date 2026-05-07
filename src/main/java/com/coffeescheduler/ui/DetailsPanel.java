package com.coffeescheduler.ui;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.Selection;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
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

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    private final Schedule schedule;
    private final ObjectProperty<Selection> selection;
    private final Consumer<Clinician> onClinicianEdited;
    private final VBox body = new VBox(8);

    public DetailsPanel(Schedule schedule, ObjectProperty<Selection> selection,
                        Consumer<Clinician> onClinicianEdited) {
        super("Details", null);
        this.schedule = schedule;
        this.selection = selection;
        this.onClinicianEdited = onClinicianEdited;
        body.setPadding(new Insets(8));
        setContent(body);
        setCollapsible(true);
        setPrefWidth(280);
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
        body.getChildren().addAll(
                heading("Week"),
                kv("Index", "Week " + week),
                kv("Date", schedule.startMonday().plusWeeks(week - 1).format(DATE)),
                kv("Coverage", on.size() + " on clinic"),
                new Label("On this week:"),
                onList);
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
        grid.setHgap(8);
        grid.setVgap(6);
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
        s.setPrefWidth(80);
        return s;
    }

    private static Label heading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        return l;
    }

    private static Label kv(String key, String value) {
        Label l = new Label(key + ": " + value);
        l.setWrapText(true);
        return l;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #888;");
        return l;
    }
}

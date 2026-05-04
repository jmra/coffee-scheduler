package com.coffeescheduler.ui;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.RuleViolation;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.Selection;
import com.coffeescheduler.model.WeekMarker;
import com.coffeescheduler.model.WeekState;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleGrid extends ScrollPane {

    private static final double CELL_HEIGHT = 24;
    private static final double CELL_WIDTH = 90;
    private static final double ROW_HEADER_WIDTH = 140;

    private static final double TRIANGLE_SIZE = 6;

    private final Schedule schedule;
    private final ObjectProperty<Selection> selection;
    private final Map<Selection.CellRef, StackPane> cellNodes = new HashMap<>();
    private final Runnable onScheduleChanged;
    private Map<Selection.CellRef, List<RuleViolation>> violationMap = Map.of();
    private Selection.CellRef anchor;

    public ScheduleGrid(Schedule schedule, ObjectProperty<Selection> selection, Runnable onScheduleChanged) {
        this.schedule = schedule;
        this.selection = selection;
        this.onScheduleChanged = onScheduleChanged;

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8));
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setFocusTraversable(true);

        for (int c = 0; c < schedule.roster().size(); c++) {
            Clinician clin = schedule.roster().get(c);
            Label header = buildHeader(clin.name(), CELL_WIDTH);
            header.setCursor(Cursor.HAND);
            header.setOnMouseClicked(e -> {
                anchor = null;
                selection.set(new Selection.OfClinician(clin));
                grid.requestFocus();
            });
            grid.add(header, c + 1, 0);
        }

        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            int week = w;
            Label rowHeader = buildRowHeader(WeekHeader.format(w, schedule.startMonday()));
            rowHeader.setCursor(Cursor.HAND);
            rowHeader.setOnMouseClicked(e -> {
                anchor = null;
                selection.set(new Selection.OfWeek(week));
                grid.requestFocus();
            });
            grid.add(rowHeader, 0, w);

            for (int c = 0; c < schedule.roster().size(); c++) {
                Clinician clin = schedule.roster().get(c);
                boolean pinned = schedule.isPinned(clin, w);
                boolean hasMarker = !schedule.markersOf(clin, w).isEmpty();
                StackPane cell = buildCell(schedule.stateOf(clin, w), pinned, hasMarker);
                cell.setCursor(Cursor.HAND);
                Selection.CellRef ref = new Selection.CellRef(clin, week);
                cell.setOnMousePressed(e -> handleCellPressed(e, ref, grid));
                cell.setOnDragDetected(e -> { cell.startFullDrag(); e.consume(); });
                cell.setOnMouseDragEntered(e -> handleCellDragEntered(ref, grid));
                cellNodes.put(ref, cell);
                grid.add(cell, c + 1, w);
            }
        }

        setOnKeyPressed(this::handleKey);
        selection.addListener((obs, oldSel, newSel) -> updateHighlight(oldSel, newSel));
        setContent(grid);
    }

    private void handleCellPressed(MouseEvent e, Selection.CellRef ref, GridPane grid) {
        if (e.getButton() == MouseButton.SECONDARY) {
            if (!(selection.get() instanceof Selection.OfCells sel) || !sel.cells().contains(ref)) {
                anchor = ref;
                selection.set(new Selection.OfCells(List.of(ref)));
            }
            showContextMenu(e, grid);
            grid.requestFocus();
            return;
        }
        if (e.isShiftDown() && anchor != null) {
            selection.set(new Selection.OfCells(buildRange(anchor, ref)));
        } else {
            anchor = ref;
            selection.set(new Selection.OfCells(List.of(ref)));
        }
        grid.requestFocus();
    }

    private void handleCellDragEntered(Selection.CellRef ref, GridPane grid) {
        if (anchor != null) {
            selection.set(new Selection.OfCells(buildRange(anchor, ref)));
        }
        grid.requestFocus();
    }

    private void showContextMenu(MouseEvent e, GridPane grid) {
        if (!(selection.get() instanceof Selection.OfCells sel) || sel.cells().isEmpty()) return;

        boolean anyPreferOn = sel.cells().stream()
                .anyMatch(r -> schedule.hasMarker(r.clinician(), r.week(), WeekMarker.PREFER_ON));
        boolean anyPreferOff = sel.cells().stream()
                .anyMatch(r -> schedule.hasMarker(r.clinician(), r.week(), WeekMarker.PREFER_OFF));

        CheckMenuItem preferOn = new CheckMenuItem("Prefer On");
        preferOn.setSelected(anyPreferOn);
        preferOn.setOnAction(ev -> {
            for (Selection.CellRef ref : sel.cells()) {
                if (anyPreferOn) {
                    schedule.removeMarker(ref.clinician(), ref.week(), WeekMarker.PREFER_ON);
                } else {
                    schedule.setMarker(ref.clinician(), ref.week(), WeekMarker.PREFER_ON);
                    schedule.removeMarker(ref.clinician(), ref.week(), WeekMarker.PREFER_OFF);
                }
                refreshCell(ref);
            }
            onScheduleChanged.run();
        });

        CheckMenuItem preferOff = new CheckMenuItem("Prefer Off");
        preferOff.setSelected(anyPreferOff);
        preferOff.setOnAction(ev -> {
            for (Selection.CellRef ref : sel.cells()) {
                if (anyPreferOff) {
                    schedule.removeMarker(ref.clinician(), ref.week(), WeekMarker.PREFER_OFF);
                } else {
                    schedule.setMarker(ref.clinician(), ref.week(), WeekMarker.PREFER_OFF);
                    schedule.removeMarker(ref.clinician(), ref.week(), WeekMarker.PREFER_ON);
                }
                refreshCell(ref);
            }
            onScheduleChanged.run();
        });

        ContextMenu menu = new ContextMenu(preferOn, preferOff);
        menu.show(grid, e.getScreenX(), e.getScreenY());
    }

    private List<Selection.CellRef> buildRange(Selection.CellRef from, Selection.CellRef to) {
        List<Clinician> roster = schedule.roster();
        int c1 = roster.indexOf(from.clinician());
        int c2 = roster.indexOf(to.clinician());
        int minC = Math.min(c1, c2);
        int maxC = Math.max(c1, c2);
        int minW = Math.min(from.week(), to.week());
        int maxW = Math.max(from.week(), to.week());

        List<Selection.CellRef> cells = new ArrayList<>();
        for (int w = minW; w <= maxW; w++) {
            for (int c = minC; c <= maxC; c++) {
                cells.add(new Selection.CellRef(roster.get(c), w));
            }
        }
        return cells;
    }

    private void handleKey(KeyEvent e) {
        switch (e.getCode()) {
            case ESCAPE -> {
                anchor = null;
                selection.set(Selection.NONE);
                e.consume();
            }
            case O, U, SPACE, DELETE, BACK_SPACE -> {
                if (selection.get() instanceof Selection.OfCells sel) {
                    clearViolations();
                    WeekState newState = switch (e.getCode()) {
                        case O -> WeekState.ON;
                        case U -> WeekState.UNAVAILABLE;
                        default -> null;
                    };
                    for (Selection.CellRef ref : sel.cells()) {
                        schedule.setState(ref.clinician(), ref.week(), newState);
                        if (newState == WeekState.ON) {
                            schedule.pin(ref.clinician(), ref.week());
                        } else {
                            schedule.unpin(ref.clinician(), ref.week());
                        }
                        refreshCell(ref);
                    }
                    onScheduleChanged.run();
                }
                e.consume();
            }
            default -> {}
        }
    }

    public void setViolations(Map<Selection.CellRef, List<RuleViolation>> violations) {
        Map<Selection.CellRef, List<RuleViolation>> old = this.violationMap;
        this.violationMap = violations;
        for (Selection.CellRef ref : old.keySet()) {
            refreshCell(ref);
        }
        for (Selection.CellRef ref : violations.keySet()) {
            refreshCell(ref);
        }
    }

    public void clearViolations() {
        if (!violationMap.isEmpty()) {
            setViolations(Map.of());
        }
    }

    private void refreshCell(Selection.CellRef ref) {
        StackPane node = cellNodes.get(ref);
        if (node != null) {
            node.setStyle(styleFor(ref, selectedCells().contains(ref)));
            updateTriangle(node, ref);
        }
    }

    private String styleFor(Selection.CellRef ref, boolean selected) {
        WeekState state = schedule.stateOf(ref.clinician(), ref.week());
        boolean pinned = schedule.isPinned(ref.clinician(), ref.week());
        boolean hasMarker = !schedule.markersOf(ref.clinician(), ref.week()).isEmpty();
        return cellStyle(state, selected, pinned, hasMarker);
    }

    private Set<Selection.CellRef> selectedCells() {
        if (selection.get() instanceof Selection.OfCells sel) {
            return new HashSet<>(sel.cells());
        }
        return Set.of();
    }

    private void updateHighlight(Selection oldSel, Selection newSel) {
        if (oldSel instanceof Selection.OfCells old) {
            for (Selection.CellRef ref : old.cells()) {
                refreshCellUnselected(ref);
            }
        }
        if (newSel instanceof Selection.OfCells sel) {
            for (Selection.CellRef ref : sel.cells()) {
                StackPane node = cellNodes.get(ref);
                if (node != null) {
                    node.setStyle(styleFor(ref, true));
                }
            }
        }
    }

    private void refreshCellUnselected(Selection.CellRef ref) {
        StackPane node = cellNodes.get(ref);
        if (node != null) {
            node.setStyle(styleFor(ref, false));
        }
    }

    private static String cellStyle(WeekState state, boolean selected, boolean pinned, boolean hasMarker) {
        String border;
        String borderWidth;
        String borderStyle = "solid";
        if (selected) {
            border = "#1976D2";
            borderWidth = "2";
        } else if (hasMarker) {
            border = "#7986cb";
            borderWidth = "1.5";
            borderStyle = "dotted";
        } else {
            border = "#d0d0d0";
            borderWidth = "0.5";
        }
        return "-fx-background-color: " + colorFor(state, pinned) + ";"
                + " -fx-border-color: " + border + ";"
                + " -fx-border-width: " + borderWidth + ";"
                + " -fx-border-style: " + borderStyle + ";";
    }

    private static StackPane buildCell(WeekState state, boolean pinned, boolean hasMarker) {
        StackPane cell = new StackPane();
        cell.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cell.setMaxSize(CELL_WIDTH, CELL_HEIGHT);
        cell.setStyle(cellStyle(state, false, pinned, hasMarker));
        return cell;
    }

    private void updateTriangle(StackPane cell, Selection.CellRef ref) {
        cell.getChildren().removeIf(n -> n instanceof Polygon);
        Tooltip.uninstall(cell, null);
        List<RuleViolation> violations = violationMap.get(ref);
        if (violations != null && !violations.isEmpty()) {
            Polygon triangle = new Polygon(0, 0, TRIANGLE_SIZE, 0, TRIANGLE_SIZE, TRIANGLE_SIZE);
            triangle.setFill(Color.web("#d32f2f"));
            triangle.setMouseTransparent(true);
            StackPane.setAlignment(triangle, Pos.TOP_RIGHT);
            cell.getChildren().add(triangle);
            String tooltipText = violations.stream()
                    .map(RuleViolation::message)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            Tooltip.install(cell, new Tooltip(tooltipText));
        }
    }

    private static Label buildHeader(String text, double width) {
        Label header = new Label(text);
        header.setPrefSize(width, CELL_HEIGHT);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
        return header;
    }

    private static Label buildRowHeader(String text) {
        Label header = new Label(text);
        header.setPrefSize(ROW_HEADER_WIDTH, CELL_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 8, 0, 8));
        header.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
        return header;
    }

    private static String colorFor(WeekState state, boolean pinned) {
        if (state == WeekState.ON) {
            return pinned ? "#66bb6a" : "#a5d6a7";
        }
        if (state == WeekState.UNAVAILABLE) {
            return "#bdbdbd";
        }
        return "#ffffff";
    }
}

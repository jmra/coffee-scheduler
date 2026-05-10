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
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleGrid extends GridPane {

    private static final double CELL_HEIGHT = 24;
    private static final double CELL_WIDTH = 90;
    private static final double ROW_HEADER_PAD = 24;

    private static final double TRIANGLE_SIZE = 6;
    private static final double MARKER_SIZE = 10;
    private static final double SCROLLBAR_WIDTH = 17;

    private final Schedule schedule;
    private final ObjectProperty<Selection> selection;
    private final Map<Selection.CellRef, StackPane> cellNodes = new HashMap<>();
    private final Runnable onScheduleChanged;
    private final Set<Integer> blockBoundaries;
    private Map<Selection.CellRef, List<RuleViolation>> violationMap = Map.of();
    private Selection.CellRef anchor;
    private ContextMenu activeContextMenu;

    public ScheduleGrid(Schedule schedule, ObjectProperty<Selection> selection, Runnable onScheduleChanged) {
        this.schedule = schedule;
        this.selection = selection;
        this.onScheduleChanged = onScheduleChanged;
        this.blockBoundaries = buildBlockBoundaries(schedule);

        double rowHeaderWidth = computeRowHeaderWidth(schedule);

        // Column header grid (clinician names)
        GridPane colHeaderGrid = new GridPane();
        colHeaderGrid.setHgap(1);
        colHeaderGrid.setVgap(1);
        colHeaderGrid.setPadding(new Insets(0, 8, 0, 0));
        for (int c = 0; c < schedule.roster().size(); c++) {
            Clinician clin = schedule.roster().get(c);
            Label header = buildHeader(clin.name(), CELL_WIDTH);
            header.setCursor(Cursor.HAND);
            header.setOnMouseClicked(e -> {
                anchor = null;
                selection.set(new Selection.OfClinician(clin));
                requestFocus();
            });
            colHeaderGrid.add(header, c, 0);
        }

        // Row header grid (week labels)
        GridPane rowHeaderGrid = new GridPane();
        rowHeaderGrid.setHgap(1);
        rowHeaderGrid.setVgap(1);
        rowHeaderGrid.setPadding(new Insets(0, 0, 8, 0));
        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            int week = w;
            int block = schedule.scheduleBlockOf(w);
            boolean lastInBlock = blockBoundaries.contains(w);
            String headerText = WeekHeader.format(w, schedule.startMonday(), schedule.lengthWeeks(), block);
            boolean hasOverride = schedule.demandFor(w) != schedule.defaultDemand();
            Label rowHeader = buildRowHeader(headerText, rowHeaderWidth, block, lastInBlock, hasOverride);
            rowHeader.setCursor(Cursor.HAND);
            rowHeader.setOnMouseClicked(e -> {
                anchor = null;
                selection.set(new Selection.OfWeek(week));
                requestFocus();
            });
            rowHeaderGrid.add(rowHeader, 0, w - 1);
        }

        // Data grid (cells only)
        GridPane dataGrid = new GridPane();
        dataGrid.setHgap(1);
        dataGrid.setVgap(1);
        dataGrid.setPadding(new Insets(0, 8, 8, 0));
        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            int week = w;
            boolean lastInBlock = blockBoundaries.contains(w);
            for (int c = 0; c < schedule.roster().size(); c++) {
                Clinician clin = schedule.roster().get(c);
                boolean pinned = schedule.isPinned(clin, w);
                Set<WeekMarker> markers = schedule.markersOf(clin, w);
                StackPane cell = buildCell(schedule.stateOf(clin, w), pinned, markers, lastInBlock);
                cell.setCursor(Cursor.HAND);
                Selection.CellRef ref = new Selection.CellRef(clin, week);
                cell.setOnMousePressed(e -> handleCellPressed(e, ref));
                cell.setOnDragDetected(e -> { cell.startFullDrag(); e.consume(); });
                cell.setOnMouseDragEntered(e -> handleCellDragEntered(ref));
                cellNodes.put(ref, cell);
                dataGrid.add(cell, c, w - 1);
            }
        }

        // ScrollPanes for each quadrant
        ScrollPane dataScroll = new ScrollPane(dataGrid);
        dataScroll.setPannable(true);
        dataScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        dataScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        ScrollPane colHeaderScroll = new ScrollPane(colHeaderGrid);
        colHeaderScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        colHeaderScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        colHeaderScroll.setFitToHeight(true);
        colHeaderScroll.setPrefHeight(CELL_HEIGHT + 2);
        colHeaderScroll.setMinHeight(CELL_HEIGHT + 2);
        colHeaderScroll.setMaxHeight(CELL_HEIGHT + 2);
        colHeaderScroll.setPadding(new Insets(0, SCROLLBAR_WIDTH, 0, 0));

        ScrollPane rowHeaderScroll = new ScrollPane(rowHeaderGrid);
        rowHeaderScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rowHeaderScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rowHeaderScroll.setPadding(new Insets(0, 0, SCROLLBAR_WIDTH, 0));
        rowHeaderScroll.setFitToWidth(true);
        rowHeaderScroll.setPrefWidth(rowHeaderWidth + 2);
        rowHeaderScroll.setMinWidth(rowHeaderWidth + 2);
        rowHeaderScroll.setMaxWidth(rowHeaderWidth + 2);

        // Synchronize scrolling
        colHeaderScroll.hvalueProperty().bind(dataScroll.hvalueProperty());
        rowHeaderScroll.vvalueProperty().bind(dataScroll.vvalueProperty());

        // Corner placeholder
        Label corner = new Label();
        corner.setPrefSize(rowHeaderWidth + 2, CELL_HEIGHT + 2);
        corner.setStyle("-fx-background-color: #e0e0e0;");

        // Layout: 2x2 grid
        add(corner, 0, 0);
        add(colHeaderScroll, 1, 0);
        add(rowHeaderScroll, 0, 1);
        add(dataScroll, 1, 1);
        GridPane.setHgrow(dataScroll, Priority.ALWAYS);
        GridPane.setVgrow(dataScroll, Priority.ALWAYS);
        GridPane.setHgrow(colHeaderScroll, Priority.ALWAYS);
        GridPane.setVgrow(rowHeaderScroll, Priority.ALWAYS);

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.SECONDARY) hideContextMenu();
        });
        setOnKeyPressed(this::handleKey);
        setFocusTraversable(true);
        selection.addListener((obs, oldSel, newSel) -> updateHighlight(oldSel, newSel));
    }

    private void hideContextMenu() {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
            activeContextMenu = null;
        }
    }

    private void handleCellPressed(MouseEvent e, Selection.CellRef ref) {
        if (e.getButton() == MouseButton.SECONDARY) {
            if (!(selection.get() instanceof Selection.OfCells sel) || !sel.cells().contains(ref)) {
                anchor = ref;
                selection.set(new Selection.OfCells(List.of(ref)));
            }
            showContextMenu(e);
            requestFocus();
            return;
        }
        if (e.isShiftDown() && anchor != null) {
            selection.set(new Selection.OfCells(buildRange(anchor, ref)));
        } else {
            anchor = ref;
            selection.set(new Selection.OfCells(List.of(ref)));
        }
        requestFocus();
    }

    private void handleCellDragEntered(Selection.CellRef ref) {
        if (anchor != null) {
            selection.set(new Selection.OfCells(buildRange(anchor, ref)));
        }
        requestFocus();
    }

    private void showContextMenu(MouseEvent e) {
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

        hideContextMenu();
        activeContextMenu = new ContextMenu(preferOn, preferOff);
        activeContextMenu.setAutoHide(true);
        activeContextMenu.show(this, e.getScreenX(), e.getScreenY());
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
            updateMarkerIndicator(node, ref);
        }
    }

    private String styleFor(Selection.CellRef ref, boolean selected) {
        WeekState state = schedule.stateOf(ref.clinician(), ref.week());
        boolean pinned = schedule.isPinned(ref.clinician(), ref.week());
        boolean lastInBlock = blockBoundaries.contains(ref.week());
        return cellStyle(state, selected, pinned, lastInBlock);
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

    private static String cellStyle(WeekState state, boolean selected, boolean pinned,
                                     boolean lastInBlock) {
        String border;
        String borderWidth;
        if (selected) {
            border = "#1976D2";
            borderWidth = "2";
        } else {
            border = "#d0d0d0";
            borderWidth = "0.5";
        }
        if (lastInBlock && !selected) {
            borderWidth = "0.5 0.5 3 0.5";
            border = border + " " + border + " #616161 " + border;
        }
        return "-fx-background-color: " + colorFor(state, pinned) + ";"
                + " -fx-border-color: " + border + ";"
                + " -fx-border-width: " + borderWidth + ";"
                + " -fx-border-style: solid;";
    }

    private static StackPane buildCell(WeekState state, boolean pinned, Set<WeekMarker> markers, boolean lastInBlock) {
        StackPane cell = new StackPane();
        cell.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        cell.setMaxSize(CELL_WIDTH, CELL_HEIGHT);
        cell.setStyle(cellStyle(state, false, pinned, lastInBlock));
        addMarkerIndicator(cell, markers);
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
        updateMarkerIndicator(cell, ref);
    }

    private static void addMarkerIndicator(StackPane cell, Set<WeekMarker> markers) {
        if (markers.contains(WeekMarker.PREFER_ON)) {
            Polygon arrow = new Polygon(0, MARKER_SIZE, MARKER_SIZE / 2, 0, MARKER_SIZE, MARKER_SIZE);
            arrow.setFill(Color.web("#424242"));
            arrow.setMouseTransparent(true);
            StackPane.setAlignment(arrow, Pos.BOTTOM_LEFT);
            cell.getChildren().add(arrow);
        } else if (markers.contains(WeekMarker.PREFER_OFF)) {
            Polygon arrow = new Polygon(0, 0, MARKER_SIZE / 2, MARKER_SIZE, MARKER_SIZE, 0);
            arrow.setFill(Color.web("#424242"));
            arrow.setMouseTransparent(true);
            StackPane.setAlignment(arrow, Pos.BOTTOM_LEFT);
            cell.getChildren().add(arrow);
        }
    }

    private void updateMarkerIndicator(StackPane cell, Selection.CellRef ref) {
        cell.getChildren().removeIf(n -> n instanceof Polygon p
                && StackPane.getAlignment(p) == Pos.BOTTOM_LEFT);
        Set<WeekMarker> markers = schedule.markersOf(ref.clinician(), ref.week());
        addMarkerIndicator(cell, markers);
    }

    private static Label buildHeader(String text, double width) {
        Label header = new Label(text);
        header.setPrefSize(width, CELL_HEIGHT);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0;");
        return header;
    }

    private static Label buildRowHeader(String text, double width, int block, boolean lastInBlock,
                                        boolean hasOverride) {
        Label header = new Label(text);
        header.setPrefSize(width, CELL_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 8, 0, 8));
        String bg = (block % 2 == 0) ? "#d4d4d4" : "#e0e0e0";
        String bottomBorder = lastInBlock ? "3" : "0.5";
        String leftBorder = hasOverride ? "3" : "0.5";
        String leftColor = hasOverride ? "#1976D2" : "#e0e0e0";
        String borderColor = lastInBlock
                ? leftColor + " #e0e0e0 #616161 " + leftColor
                : leftColor + " #e0e0e0 #e0e0e0 " + leftColor;
        header.setStyle("-fx-font-weight: bold; -fx-background-color: " + bg + ";"
                + " -fx-border-color: " + borderColor + ";"
                + " -fx-border-width: 0.5 0.5 " + bottomBorder + " " + leftBorder + ";");
        return header;
    }

    private static double computeRowHeaderWidth(Schedule schedule) {
        Text measure = new Text();
        measure.setFont(Font.font("System", 13));
        double max = 0;
        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            int block = schedule.scheduleBlockOf(w);
            measure.setText(WeekHeader.format(w, schedule.startMonday(), schedule.lengthWeeks(), block));
            double tw = measure.getLayoutBounds().getWidth();
            if (tw > max) max = tw;
        }
        return max + ROW_HEADER_PAD;
    }

    private static Set<Integer> buildBlockBoundaries(Schedule schedule) {
        Set<Integer> boundaries = new HashSet<>();
        int cumulative = 0;
        List<Integer> sizes = schedule.scheduleBlockSizes();
        for (int i = 0; i < sizes.size() - 1; i++) {
            cumulative += sizes.get(i);
            boundaries.add(cumulative);
        }
        return boundaries;
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

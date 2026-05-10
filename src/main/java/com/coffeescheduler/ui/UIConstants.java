package com.coffeescheduler.ui;

import javafx.geometry.Insets;

public final class UIConstants {

    private UIConstants() {}

    // --- Colors (hex strings for CSS / Color.web) ---

    // Cell state
    public static final String COLOR_ON_PINNED = "#66bb6a";
    public static final String COLOR_ON_UNPINNED = "#a5d6a7";
    public static final String COLOR_UNAVAILABLE = "#bdbdbd";
    public static final String COLOR_OFF = "#ffffff";

    // Grid chrome
    public static final String COLOR_HEADER_BG = "#e0e0e0";
    public static final String COLOR_ROW_ALT_EVEN = "#d4d4d4";
    public static final String COLOR_ROW_ALT_ODD = COLOR_HEADER_BG;
    public static final String COLOR_BORDER = "#d0d0d0";
    public static final String COLOR_BLOCK_SEPARATOR = "#616161";

    // Accent
    public static final String COLOR_SELECTION = "#1976D2";
    public static final String COLOR_VIOLATION = "#d32f2f";
    public static final String COLOR_MARKER = "#424242";

    // Text
    public static final String COLOR_MUTED_TEXT = "#888";
    public static final String COLOR_LINK = COLOR_SELECTION;

    // Excel RGB byte arrays (must match the hex colors above)
    public static final byte[] EXCEL_COLOR_ON = {(byte) 0xa5, (byte) 0xd6, (byte) 0xa7};
    public static final byte[] EXCEL_COLOR_UNAVAILABLE = {(byte) 0xbd, (byte) 0xbd, (byte) 0xbd};
    public static final byte[] EXCEL_COLOR_OFF = {(byte) 0xff, (byte) 0xff, (byte) 0xff};
    public static final byte[] EXCEL_COLOR_HEADER = {(byte) 0xe0, (byte) 0xe0, (byte) 0xe0};

    // --- Grid / cell dimensions ---

    public static final double CELL_HEIGHT = 24;
    public static final double CELL_WIDTH = 90;
    public static final double ROW_HEADER_PAD = 24;
    public static final double TRIANGLE_SIZE = 6;
    public static final double MARKER_SIZE = 10;
    public static final double SCROLLBAR_WIDTH = 17;

    // --- Panel dimensions ---

    public static final double ROSTER_PANEL_WIDTH = 220;
    public static final double RULES_PANEL_WIDTH = 220;
    public static final double DETAILS_PANEL_WIDTH = 280;
    public static final double VIOLATIONS_SCROLL_HEIGHT = 120;
    public static final double SPINNER_WIDTH = 80;
    public static final double BLOCK_SIZES_FIELD_WIDTH = 160;

    // --- Window ---

    public static final double WINDOW_WIDTH = 1200;
    public static final double WINDOW_HEIGHT = 800;

    // --- Spacing ---

    public static final double GRID_GAP = 1;
    public static final double PANEL_SPACING = 8;
    public static final double STATUS_BAR_SPACING = 12;
    public static final double DIALOG_HGAP = 12;
    public static final double DIALOG_VGAP = 8;
    public static final double DIALOG_PADDING = 16;
    public static final double DETAIL_GRID_HGAP = 8;
    public static final double DETAIL_GRID_VGAP = 6;
    public static final double VIOLATIONS_ROW_SPACING = 8;
    public static final double CHECKBOX_SPACING = 4;

    public static final Insets PANEL_INSETS = new Insets(8);
    public static final Insets STATUS_BAR_INSETS = new Insets(4, 8, 4, 8);
    public static final Insets ROW_HEADER_CELL_INSETS = new Insets(0, 8, 0, 8);
    public static final Insets VIOLATIONS_ROW_INSETS = new Insets(2, 4, 2, 4);

    // --- Typography ---

    public static final String FONT_FAMILY = "System";
    public static final double FONT_SIZE = 13;
    public static final double FONT_SIZE_PROMPT = 14;
    public static final double FONT_SIZE_ERROR = 11;

    // --- Excel column widths ---

    public static final int EXCEL_WEEK_COLUMN_WIDTH = 5000;
    public static final int EXCEL_CLINICIAN_COLUMN_WIDTH = 3500;

    // --- Date format patterns ---

    public static final String DATE_PATTERN_DETAIL = "EEE, MMM d, yyyy";
    public static final String DATE_PATTERN_WEEK_HEADER = "EEE MMM d";
}

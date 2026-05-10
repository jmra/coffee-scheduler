# Coffee Scheduler

A cross-platform desktop application for scheduling a roster of clinicians across a user-defined span of weeks.

## Overview

The user defines a roster of clinicians and a schedule whose length is set in whole weeks (typically a year, but not required). The application can auto-generate a schedule subject to a set of rules and preferences (see below), the user can manually edit any generated schedule, and the result can be exported as an Excel spreadsheet.

Created using Claude Code, Opus 4.6

## Stack

- **Java 25**
- **Maven** (build)
- **JavaFX** (UI)
- **Jackson** (JSON persistence)
- **Apache POI** (Excel export)

## Building and running

Requires JDK 25+ and a desktop environment (X11/Wayland on Linux, or native on macOS/Windows).

```bash
mvn javafx:run        # launch the app
mvn test              # run all tests (201 as of 2026-05-09)
```

## Packaging a native executable

The `scripts/package.sh` script builds a self-contained native application image using `jlink` + `jpackage`. The output includes a minimal JDK runtime (~61 MB) so end users don't need Java installed.

**Prerequisites:** JDK 25+ with `jlink` and `jpackage` on `PATH`.

```bash
./scripts/package.sh
```

This will:
1. Build the project with Maven (`mvn clean package -DskipTests`)
2. Create a stripped-down JDK runtime via `jlink` (only the modules the app needs)
3. Package the app + runtime into a native image via `jpackage`

Output lands in `target/dist/`. On Linux this is an app-image directory; on macOS a `.app` bundle; on Windows a directory (change `TYPE` in the script to `msi` or `exe` for an installer).

**Cross-platform note:** `jpackage` builds for the platform it runs on. To produce a Windows executable, run the script on Windows (Git Bash / MSYS2). To produce a Linux image, run on Linux.

## Scope

- Schedule granularity: **one week** is the smallest slot. Weeks start on **Monday**.
- Schedule length: user-defined whole number of weeks (start Monday + N weeks)
- Roster: **<20 clinicians**, user-managed
- Per-week shape: a **subset** of clinicians is on clinic each week (single clinic, multi-person)
- Persistence: local JSON files, document-style (open / save a single `.json` schedule file)
- Schedule generation: handwritten algorithm (no constraint solver)
- Export: `.xlsx` via Apache POI

### Keyboard shortcuts
- `Ctrl+N` New, `Ctrl+O` Open, `Ctrl+S` Save, `Ctrl+Shift+S` Save As, `Ctrl+Q` Quit
- `Ctrl+E` Export to Excel
- `Ctrl+G` Generate, `Ctrl+Shift+G` Check violations
- `Ctrl+F` Find clinician
- `Esc` Clear selection
- `O` / `U` / `Space` set state on selection
- `Delete` clear selection state

## Source layout

```
src/main/java/com/coffeescheduler/
├── model/
│   ├── Block.java              Record: (startWeek, length)
│   ├── BlockLengthRange.java   Record: preferred block min/max
│   ├── Clinician.java          Record: name, contracted weeks, block config
│   ├── ContractedWeeks.java    Record: contracted weeks min/max
│   ├── DemandOverride.java     Record: per-span demand override (startWeek, endWeek, demand)
│   ├── ExclusionGroup.java     Record: named group of mutually-exclusive clinicians
│   ├── InclusionGroup.java     Record: named group where ≥1 must be ON each week
│   ├── RuleViolation.java      Record: (message, clinician?, week?)
│   ├── Schedule.java           Core model: weeks × clinicians grid + settings
│   ├── Selection.java          Sealed interface: None/OfCells/OfWeek/OfClinician
│   ├── WeeklyDemand.java       Record: demand min/ideal/max
│   ├── WeekMarker.java         Enum: PREFER_ON, PREFER_OFF
│   └── WeekState.java          Enum: ON, UNAVAILABLE
├── generator/
│   ├── ConstructiveGenerator.java  Phase 1 greedy schedule generator
│   ├── GeneratorResult.java        (Schedule, List<RuleViolation>) pair
│   ├── LocalSearchImprover.java    Phase 2 hill-climbing swap optimizer
│   ├── ScheduleGenerator.java      Interface for generator implementations
│   ├── ScheduleScorer.java         Full-schedule scorer (violations + soft score)
│   └── TwoPhaseGenerator.java      Composes phase 1 constructive + phase 2 local search
├── ui/
│   ├── Launcher.java           Plain main class for jpackage
│   ├── App.java                JavaFX Application entry point
│   ├── MainWindow.java         BorderPane shell, menu wiring, file operations
│   ├── ScheduleGrid.java       Grid display + cell selection/editing
│   ├── DetailsPanel.java       Context-sensitive right panel (4 modes)
│   ├── RosterPanel.java        Left panel: clinician list + add/remove
│   ├── RulesPanel.java         Left panel: collapsible rules list (exclusion + inclusion groups)
│   ├── GroupDialog.java        Modal: add/edit exclusion or inclusion group
│   ├── NewScheduleDialog.java  Modal: create schedule with settings
│   ├── ScheduleSettingsDialog.java  Modal: edit schedule settings
│   ├── AddClinicianDialog.java Modal: add clinician with full config
│   ├── ViolationsPanel.java    Expandable violations list with Jump links
│   ├── WeekHeader.java         Week label formatter
│   └── ScheduleSummary.java    Status bar summary formatter
└── io/
    ├── ScheduleJson.java       Jackson-based JSON serialization
    └── ExcelExporter.java      .xlsx export via Apache POI
```
package com.coffeescheduler.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Schedule {

    private static final int DEFAULT_REST_WEEKS = 2;
    private static final int DEFAULT_MIN_BLOCK_LENGTH = 2;

    private LocalDate startMonday;
    private int lengthWeeks;
    private final List<Clinician> roster;
    private WeeklyDemand defaultDemand;
    private int restWeeks;
    private int minBlockLength;
    private final Map<Cell, WeekState> states = new HashMap<>();
    private final Map<Cell, EnumSet<WeekMarker>> markers = new HashMap<>();
    private final Set<Cell> pinnedCells = new HashSet<>();

    public Schedule(LocalDate startMonday, int lengthWeeks, List<Clinician> roster) {
        this(startMonday, lengthWeeks, roster, new WeeklyDemand(2, 3, 5), DEFAULT_REST_WEEKS);
    }

    public Schedule(LocalDate startMonday, int lengthWeeks, List<Clinician> roster,
                    WeeklyDemand defaultDemand, int restWeeks) {
        this.startMonday = startMonday;
        this.lengthWeeks = lengthWeeks;
        this.roster = new ArrayList<>(roster);
        this.defaultDemand = defaultDemand;
        this.restWeeks = restWeeks;
        this.minBlockLength = DEFAULT_MIN_BLOCK_LENGTH;
    }

    public LocalDate startMonday() {
        return startMonday;
    }

    public int lengthWeeks() {
        return lengthWeeks;
    }

    public List<Clinician> roster() {
        return Collections.unmodifiableList(roster);
    }

    public WeeklyDemand defaultDemand() {
        return defaultDemand;
    }

    public int restWeeks() {
        return restWeeks;
    }

    public int minBlockLength() {
        return minBlockLength;
    }

    public void setStartMonday(LocalDate startMonday) {
        this.startMonday = startMonday;
    }

    public void setLengthWeeks(int lengthWeeks) {
        if (lengthWeeks < this.lengthWeeks) {
            states.keySet().removeIf(cell -> cell.week() > lengthWeeks);
            markers.keySet().removeIf(cell -> cell.week() > lengthWeeks);
            pinnedCells.removeIf(cell -> cell.week() > lengthWeeks);
        }
        this.lengthWeeks = lengthWeeks;
    }

    public void setDefaultDemand(WeeklyDemand defaultDemand) {
        this.defaultDemand = defaultDemand;
    }

    public void setRestWeeks(int restWeeks) {
        this.restWeeks = restWeeks;
    }

    public void addClinician(Clinician clinician) {
        for (Clinician existing : roster) {
            if (existing.name().equals(clinician.name())) {
                throw new IllegalArgumentException("clinician name already in roster: " + clinician.name());
            }
        }
        roster.add(clinician);
    }

    public void replaceClinician(Clinician old, Clinician replacement) {
        int idx = roster.indexOf(old);
        if (idx < 0) {
            throw new IllegalArgumentException("clinician not in roster: " + old.name());
        }
        if (!old.name().equals(replacement.name())) {
            for (Clinician existing : roster) {
                if (existing != old && existing.name().equals(replacement.name())) {
                    throw new IllegalArgumentException("clinician name already in roster: " + replacement.name());
                }
            }
        }
        roster.set(idx, replacement);
        migrateKeys(old, replacement);
    }

    private void migrateKeys(Clinician old, Clinician replacement) {
        for (int w = 1; w <= lengthWeeks; w++) {
            Cell oldCell = new Cell(old, w);
            Cell newCell = new Cell(replacement, w);
            WeekState s = states.remove(oldCell);
            if (s != null) states.put(newCell, s);
            EnumSet<WeekMarker> m = markers.remove(oldCell);
            if (m != null) markers.put(newCell, m);
            if (pinnedCells.remove(oldCell)) pinnedCells.add(newCell);
        }
    }

    public void removeClinician(Clinician clinician) {
        if (!roster.remove(clinician)) {
            throw new IllegalArgumentException("clinician not in roster: " + clinician.name());
        }
        states.keySet().removeIf(cell -> cell.clinician().equals(clinician));
        markers.keySet().removeIf(cell -> cell.clinician().equals(clinician));
        pinnedCells.removeIf(cell -> cell.clinician().equals(clinician));
    }

    public void setState(Clinician clinician, int week, WeekState state) {
        Cell cell = new Cell(clinician, week);
        if (state == null) {
            states.remove(cell);
        } else {
            states.put(cell, state);
        }
    }

    public WeekState stateOf(Clinician clinician, int week) {
        return states.get(new Cell(clinician, week));
    }

    public void pin(Clinician clinician, int week) {
        pinnedCells.add(new Cell(clinician, week));
    }

    public void unpin(Clinician clinician, int week) {
        pinnedCells.remove(new Cell(clinician, week));
    }

    public boolean isPinned(Clinician clinician, int week) {
        return pinnedCells.contains(new Cell(clinician, week));
    }

    public void clearAllPins() {
        pinnedCells.clear();
    }

    public void setMarker(Clinician clinician, int week, WeekMarker marker) {
        markers.computeIfAbsent(new Cell(clinician, week), k -> EnumSet.noneOf(WeekMarker.class)).add(marker);
    }

    public void removeMarker(Clinician clinician, int week, WeekMarker marker) {
        Cell cell = new Cell(clinician, week);
        EnumSet<WeekMarker> set = markers.get(cell);
        if (set != null) {
            set.remove(marker);
            if (set.isEmpty()) {
                markers.remove(cell);
            }
        }
    }

    public boolean hasMarker(Clinician clinician, int week, WeekMarker marker) {
        EnumSet<WeekMarker> set = markers.get(new Cell(clinician, week));
        return set != null && set.contains(marker);
    }

    public Set<WeekMarker> markersOf(Clinician clinician, int week) {
        EnumSet<WeekMarker> set = markers.get(new Cell(clinician, week));
        return set == null ? Set.of() : Set.copyOf(set);
    }

    public void swapAssignment(int week, Clinician a, Clinician b) {
        Cell cellA = new Cell(a, week);
        Cell cellB = new Cell(b, week);
        WeekState stateA = states.get(cellA);
        WeekState stateB = states.get(cellB);
        putOrRemove(cellA, stateB);
        putOrRemove(cellB, stateA);
    }

    private void putOrRemove(Cell cell, WeekState state) {
        if (state == null) {
            states.remove(cell);
        } else {
            states.put(cell, state);
        }
    }

    public List<Block> blocksFor(Clinician clinician) {
        List<Block> blocks = new ArrayList<>();
        int blockStart = -1;
        for (int week = 1; week <= lengthWeeks; week++) {
            boolean on = states.get(new Cell(clinician, week)) == WeekState.ON;
            if (on && blockStart == -1) {
                blockStart = week;
            } else if (!on && blockStart != -1) {
                blocks.add(new Block(blockStart, week - blockStart));
                blockStart = -1;
            }
        }
        if (blockStart != -1) {
            blocks.add(new Block(blockStart, lengthWeeks - blockStart + 1));
        }
        return blocks;
    }

    public Set<Clinician> onClinicians(int week) {
        Set<Clinician> on = new LinkedHashSet<>();
        for (Clinician c : roster) {
            if (states.get(new Cell(c, week)) == WeekState.ON) {
                on.add(c);
            }
        }
        return on;
    }

    private record Cell(Clinician clinician, int week) {}
}

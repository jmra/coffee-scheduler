package com.coffeescheduler.model;

import java.util.List;

public sealed interface Selection {

    Selection NONE = new None();

    record None() implements Selection {}

    record OfCells(List<CellRef> cells) implements Selection {
        public OfCells {
            cells = List.copyOf(cells);
        }
    }

    record OfWeek(int week) implements Selection {}

    record OfClinician(Clinician clinician) implements Selection {}

    record CellRef(Clinician clinician, int week) {}
}

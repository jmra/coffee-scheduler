package com.coffeescheduler.ui;

import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeeklyDemand;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class NewScheduleDialog extends Dialog<Schedule> {

    public NewScheduleDialog() {
        setTitle("New Schedule");
        setHeaderText("Create a new schedule");

        DatePicker startDate = new DatePicker(nextMonday());
        startDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(date.getDayOfWeek() != DayOfWeek.MONDAY);
            }
        });

        Spinner<Integer> weeksSpinner = intSpinner(1, 104, 52);
        Spinner<Integer> demandMin = intSpinner(0, 20, 2);
        Spinner<Integer> demandIdeal = intSpinner(0, 20, 3);
        Spinner<Integer> demandMax = intSpinner(0, 20, 5);
        Spinner<Integer> restWeeks = intSpinner(1, 12, 2);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        int row = 0;
        grid.add(new Label("Start Monday:"), 0, row);
        grid.add(startDate, 1, row++);
        grid.add(new Label("Weeks:"), 0, row);
        grid.add(weeksSpinner, 1, row++);
        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(new Label("Demand per week (min):"), 0, row);
        grid.add(demandMin, 1, row++);
        grid.add(new Label("Demand per week (ideal):"), 0, row);
        grid.add(demandIdeal, 1, row++);
        grid.add(new Label("Demand per week (max):"), 0, row);
        grid.add(demandMax, 1, row++);
        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(new Label("Rest weeks between blocks:"), 0, row);
        grid.add(restWeeks, 1, row);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                LocalDate monday = startDate.getValue();
                if (monday != null && monday.getDayOfWeek() == DayOfWeek.MONDAY) {
                    WeeklyDemand demand = new WeeklyDemand(
                            demandMin.getValue(), demandIdeal.getValue(), demandMax.getValue());
                    return new Schedule(monday, weeksSpinner.getValue(), List.of(),
                            demand, restWeeks.getValue());
                }
            }
            return null;
        });
    }

    private static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> s = new Spinner<>();
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        s.setEditable(true);
        s.setPrefWidth(80);
        return s;
    }

    private static LocalDate nextMonday() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        if (daysUntilMonday == 0) return today;
        return today.plusDays(daysUntilMonday);
    }

    public static Optional<Schedule> prompt() {
        return new NewScheduleDialog().showAndWait();
    }
}

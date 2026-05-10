package com.coffeescheduler.ui;

import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeeklyDemand;
import javafx.geometry.Insets;
import javafx.scene.Node;
//import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
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
        TextField blockSizesField = new TextField("52");
        blockSizesField.setPromptText("e.g. 4,4,4");
        blockSizesField.setPrefWidth(160);
        Label blockError = new Label();
        blockError.setStyle("-fx-text-fill: red; -fx-font-size: 11;");
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
        grid.add(new Label("Schedule blocks:"), 0, row);
        grid.add(blockSizesField, 1, row++);
        grid.add(blockError, 1, row++);
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
        Node okButton = getDialogPane().lookupButton(ButtonType.OK);

        Runnable validate = () -> {
            String err = validateBlockSizes(blockSizesField.getText(), weeksSpinner.getValue());
            blockError.setText(err);
            okButton.setDisable(!err.isEmpty());
        };
        blockSizesField.textProperty().addListener((obs, o, n) -> validate.run());
        weeksSpinner.valueProperty().addListener((obs, o, n) -> {
            blockSizesField.setText(String.valueOf(n));
            validate.run();
        });
        validate.run();

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                LocalDate monday = startDate.getValue();
                if (monday != null && monday.getDayOfWeek() == DayOfWeek.MONDAY) {
                    WeeklyDemand demand = new WeeklyDemand(
                            demandMin.getValue(), demandIdeal.getValue(), demandMax.getValue());
                    Schedule s = new Schedule(monday, weeksSpinner.getValue(), List.of(),
                            demand, restWeeks.getValue());
                    s.setScheduleBlockSizes(parseBlockSizes(blockSizesField.getText()));
                    return s;
                }
            }
            return null;
        });
    }

    static String validateBlockSizes(String text, int totalWeeks) {
        if (text == null || text.isBlank()) return "Block sizes required";
        String[] parts = text.split(",");
        int sum = 0;
        for (String part : parts) {
            try {
                int val = Integer.parseInt(part.strip());
                if (val <= 0) return "Each block size must be positive";
                sum += val;
            } catch (NumberFormatException e) {
                return "Invalid number: " + part.strip();
            }
        }
        if (sum != totalWeeks) return "Blocks sum to " + sum + ", but schedule is " + totalWeeks + " weeks";
        return "";
    }

    static List<Integer> parseBlockSizes(String text) {
        List<Integer> sizes = new ArrayList<>();
        for (String part : text.split(",")) {
            sizes.add(Integer.parseInt(part.strip()));
        }
        return sizes;
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

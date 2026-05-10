package com.coffeescheduler.ui;

import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeeklyDemand;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
import java.util.stream.Collectors;

public class ScheduleSettingsDialog extends Dialog<Boolean> {

    public ScheduleSettingsDialog(Schedule schedule) {
        setTitle("Schedule Settings");
        setHeaderText("Edit schedule settings");

        DatePicker startDate = new DatePicker(schedule.startMonday());
        startDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(date.getDayOfWeek() != DayOfWeek.MONDAY);
            }
        });

        Spinner<Integer> weeksSpinner = intSpinner(1, 104, schedule.lengthWeeks());
        String currentBlocks = schedule.scheduleBlockSizes().stream()
                .map(String::valueOf).collect(Collectors.joining(","));
        TextField blockSizesField = new TextField(currentBlocks);
        blockSizesField.setPromptText("e.g. 4,4,4");
        blockSizesField.setPrefWidth(UIConstants.BLOCK_SIZES_FIELD_WIDTH);
        Label blockError = new Label();
        blockError.setStyle("-fx-text-fill: red; -fx-font-size: " + (int) UIConstants.FONT_SIZE_ERROR + ";");
        Spinner<Integer> demandMin = intSpinner(0, 20, schedule.defaultDemand().min());
        Spinner<Integer> demandIdeal = intSpinner(0, 20, schedule.defaultDemand().ideal());
        Spinner<Integer> demandMax = intSpinner(0, 20, schedule.defaultDemand().max());
        Spinner<Integer> restWeeks = intSpinner(1, 12, schedule.restWeeks());

        GridPane grid = new GridPane();
        grid.setHgap(UIConstants.DIALOG_HGAP);
        grid.setVgap(UIConstants.DIALOG_VGAP);
        grid.setPadding(new Insets(UIConstants.DIALOG_PADDING));
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
            String err = NewScheduleDialog.validateBlockSizes(blockSizesField.getText(), weeksSpinner.getValue());
            blockError.setText(err);
            okButton.setDisable(!err.isEmpty());
        };
        blockSizesField.textProperty().addListener((obs, o, n) -> validate.run());
        weeksSpinner.valueProperty().addListener((obs, o, n) -> validate.run());
        validate.run();

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                LocalDate monday = startDate.getValue();
                if (monday != null && monday.getDayOfWeek() == DayOfWeek.MONDAY) {
                    schedule.setStartMonday(monday);
                    schedule.setLengthWeeks(weeksSpinner.getValue());
                    schedule.setScheduleBlockSizes(NewScheduleDialog.parseBlockSizes(blockSizesField.getText()));
                    schedule.setDefaultDemand(new WeeklyDemand(
                            demandMin.getValue(), demandIdeal.getValue(), demandMax.getValue()));
                    schedule.setRestWeeks(restWeeks.getValue());
                    return true;
                }
            }
            return null;
        });
    }

    private static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> s = new Spinner<>();
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        s.setEditable(true);
        s.setPrefWidth(UIConstants.SPINNER_WIDTH);
        return s;
    }
}

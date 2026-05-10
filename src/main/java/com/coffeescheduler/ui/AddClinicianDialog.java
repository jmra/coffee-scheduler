package com.coffeescheduler.ui;

import com.coffeescheduler.model.BlockLengthRange;
import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ContractedWeeks;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class AddClinicianDialog extends Dialog<Clinician> {

    public AddClinicianDialog() {
        setTitle("Add Clinician");
        setHeaderText("Enter clinician details");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Dr. Smith");

        Spinner<Integer> contractMin = intSpinner(0, 52, 20);
        Spinner<Integer> contractMax = intSpinner(0, 52, 24);
        Spinner<Integer> maxBlock = intSpinner(2, 26, 6);
        Spinner<Integer> maxBlocksAtMax = intSpinner(0, 26, 2);
        Spinner<Integer> prefMin = intSpinner(2, 26, 4);
        Spinner<Integer> prefMax = intSpinner(2, 26, 5);

        GridPane grid = new GridPane();
        grid.setHgap(UIConstants.DIALOG_HGAP);
        grid.setVgap(UIConstants.DIALOG_VGAP);
        grid.setPadding(new Insets(UIConstants.DIALOG_PADDING));
        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Contracted weeks min:"), 0, row);
        grid.add(contractMin, 1, row++);
        grid.add(new Label("Contracted weeks max:"), 0, row);
        grid.add(contractMax, 1, row++);
        grid.add(new Label("Max block length:"), 0, row);
        grid.add(maxBlock, 1, row++);
        grid.add(new Label("Max blocks at max length:"), 0, row);
        grid.add(maxBlocksAtMax, 1, row++);
        grid.add(new Label("Preferred block min:"), 0, row);
        grid.add(prefMin, 1, row++);
        grid.add(new Label("Preferred block max:"), 0, row);
        grid.add(prefMax, 1, row);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == ButtonType.OK && !nameField.getText().isBlank()) {
                return new Clinician(
                        nameField.getText().strip(),
                        new ContractedWeeks(contractMin.getValue(), contractMax.getValue()),
                        maxBlock.getValue(),
                        maxBlocksAtMax.getValue(),
                        new BlockLengthRange(prefMin.getValue(), prefMax.getValue()));
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

    public static Optional<Clinician> prompt() {
        return new AddClinicianDialog().showAndWait();
    }
}

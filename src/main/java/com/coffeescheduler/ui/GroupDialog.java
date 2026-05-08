package com.coffeescheduler.ui;

import com.coffeescheduler.model.Clinician;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupDialog extends Dialog<GroupDialog.Result> {

    public record Result(String name, Set<String> members) {}

    public enum GroupType { EXCLUSION, INCLUSION }

    public GroupDialog(List<Clinician> roster, GroupType type, String existingName, Set<String> existingMembers) {
        String label = type == GroupType.EXCLUSION ? "Exclusion" : "Inclusion";
        boolean editing = existingName != null;
        setTitle(editing ? "Edit " + label + " Group" : "Add " + label + " Group");
        setHeaderText(type == GroupType.EXCLUSION
                ? "Select clinicians who cannot work the same week."
                : "Select clinicians where at least one must work each week.");

        TextField nameField = new TextField();
        nameField.setPromptText("Group name");
        if (editing) {
            nameField.setText(existingName);
        }

        List<CheckBox> checkboxes = new ArrayList<>();
        VBox checkboxContainer = new VBox(4);
        for (Clinician c : roster) {
            CheckBox cb = new CheckBox(c.name());
            if (existingMembers != null && existingMembers.contains(c.name())) {
                cb.setSelected(true);
            }
            checkboxes.add(cb);
            checkboxContainer.getChildren().add(cb);
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        VBox content = new VBox(8,
                new Label("Name:"), nameField,
                new Label("Members:"), checkboxContainer,
                errorLabel);
        content.setPadding(new Insets(16));

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    String name = nameField.getText() == null ? "" : nameField.getText().strip();
                    Set<String> selected = new LinkedHashSet<>();
                    for (CheckBox cb : checkboxes) {
                        if (cb.isSelected()) selected.add(cb.getText());
                    }
                    if (name.isBlank()) {
                        errorLabel.setText("Name is required.");
                        event.consume();
                    } else if (selected.size() < 2) {
                        errorLabel.setText("Select at least 2 clinicians.");
                        event.consume();
                    }
                });

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String name = nameField.getText().strip();
                Set<String> selected = new LinkedHashSet<>();
                for (CheckBox cb : checkboxes) {
                    if (cb.isSelected()) selected.add(cb.getText());
                }
                return new Result(name, selected);
            }
            return null;
        });
    }
}

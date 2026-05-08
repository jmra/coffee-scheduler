package com.coffeescheduler.ui;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.ExclusionGroup;
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

public class ExclusionGroupDialog extends Dialog<ExclusionGroup> {

    public ExclusionGroupDialog(List<Clinician> roster, ExclusionGroup existing) {
        setTitle(existing == null ? "Add Exclusion Group" : "Edit Exclusion Group");
        setHeaderText("Select clinicians who cannot work the same week.");

        TextField nameField = new TextField();
        nameField.setPromptText("Group name");
        if (existing != null) {
            nameField.setText(existing.name());
        }

        List<CheckBox> checkboxes = new ArrayList<>();
        VBox checkboxContainer = new VBox(4);
        for (Clinician c : roster) {
            CheckBox cb = new CheckBox(c.name());
            if (existing != null && existing.members().contains(c.name())) {
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
                return new ExclusionGroup(name, selected);
            }
            return null;
        });
    }
}

package com.coffeescheduler.ui;

import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.Schedule;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RulesPanel extends TitledPane {

    private Schedule schedule;
    private final ListView<ExclusionGroup> list = new ListView<>();
    private final Button addBtn = new Button("Add");
    private final Button editBtn = new Button("Edit");
    private final Button removeBtn = new Button("Remove");
    private final Runnable onRulesChanged;

    public RulesPanel(Runnable onRulesChanged) {
        super("Rules", null);
        this.onRulesChanged = onRulesChanged;

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ExclusionGroup item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name() + " (" + String.join(", ", item.members()) + ")");
                }
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);

        addBtn.setOnAction(e -> addGroup());
        editBtn.setOnAction(e -> editGroup());
        removeBtn.setOnAction(e -> removeGroup());
        addBtn.setDisable(true);
        editBtn.setDisable(true);
        removeBtn.setDisable(true);

        list.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            editBtn.setDisable(sel == null);
            removeBtn.setDisable(sel == null);
        });

        VBox content = new VBox(8, new HBox(8, addBtn, editBtn, removeBtn), list);
        content.setPadding(new Insets(8));
        setContent(content);
        setCollapsible(true);
        setExpanded(false);
        setPrefWidth(220);
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        addBtn.setDisable(schedule == null);
        editBtn.setDisable(true);
        removeBtn.setDisable(true);
        refreshList();
    }

    public void refresh() {
        refreshList();
    }

    private void refreshList() {
        list.getItems().setAll(schedule == null ? java.util.List.of() : schedule.exclusionGroups());
    }

    private void addGroup() {
        if (schedule == null || schedule.roster().size() < 2) {
            new Alert(Alert.AlertType.WARNING, "Need at least 2 clinicians to create an exclusion group.")
                    .showAndWait();
            return;
        }
        ExclusionGroupDialog dialog = new ExclusionGroupDialog(schedule.roster(), null);
        dialog.showAndWait().ifPresent(group -> {
            try {
                schedule.addExclusionGroup(group);
                refreshList();
                onRulesChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void editGroup() {
        ExclusionGroup selected = list.getSelectionModel().getSelectedItem();
        if (schedule == null || selected == null) return;
        ExclusionGroupDialog dialog = new ExclusionGroupDialog(schedule.roster(), selected);
        dialog.showAndWait().ifPresent(updated -> {
            try {
                schedule.replaceExclusionGroup(selected.name(), updated);
                refreshList();
                onRulesChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void removeGroup() {
        ExclusionGroup selected = list.getSelectionModel().getSelectedItem();
        if (schedule == null || selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove exclusion group '" + selected.name() + "'?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                schedule.removeExclusionGroup(selected.name());
                refreshList();
                onRulesChanged.run();
            }
        });
    }
}

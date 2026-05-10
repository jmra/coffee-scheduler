package com.coffeescheduler.ui;

import com.coffeescheduler.model.ExclusionGroup;
import com.coffeescheduler.model.InclusionGroup;
import com.coffeescheduler.model.Schedule;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RulesPanel extends TitledPane {

    private Schedule schedule;
    private final ListView<ExclusionGroup> exclusionList = new ListView<>();
    private final Button excAddBtn = new Button("Add");
    private final Button excEditBtn = new Button("Edit");
    private final Button excRemoveBtn = new Button("Remove");

    private final ListView<InclusionGroup> inclusionList = new ListView<>();
    private final Button incAddBtn = new Button("Add");
    private final Button incEditBtn = new Button("Edit");
    private final Button incRemoveBtn = new Button("Remove");

    private final Runnable onRulesChanged;

    public RulesPanel(Runnable onRulesChanged) {
        super("Rules", null);
        this.onRulesChanged = onRulesChanged;

        exclusionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ExclusionGroup item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.name() + " (" + String.join(", ", item.members()) + ")");
            }
        });
        VBox.setVgrow(exclusionList, Priority.ALWAYS);

        excAddBtn.setOnAction(e -> addExclusionGroup());
        excEditBtn.setOnAction(e -> editExclusionGroup());
        excRemoveBtn.setOnAction(e -> removeExclusionGroup());
        excAddBtn.setDisable(true);
        excEditBtn.setDisable(true);
        excRemoveBtn.setDisable(true);

        exclusionList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            excEditBtn.setDisable(sel == null);
            excRemoveBtn.setDisable(sel == null);
        });

        inclusionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(InclusionGroup item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.name() + " (" + String.join(", ", item.members()) + ")");
            }
        });
        VBox.setVgrow(inclusionList, Priority.ALWAYS);

        incAddBtn.setOnAction(e -> addInclusionGroup());
        incEditBtn.setOnAction(e -> editInclusionGroup());
        incRemoveBtn.setOnAction(e -> removeInclusionGroup());
        incAddBtn.setDisable(true);
        incEditBtn.setDisable(true);
        incRemoveBtn.setDisable(true);

        inclusionList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            incEditBtn.setDisable(sel == null);
            incRemoveBtn.setDisable(sel == null);
        });

        Label excLabel = new Label("Exclusion Groups");
        excLabel.setStyle("-fx-font-weight: bold;");
        Label incLabel = new Label("Inclusion Groups");
        incLabel.setStyle("-fx-font-weight: bold;");

        VBox content = new VBox(UIConstants.PANEL_SPACING,
                excLabel,
                new HBox(UIConstants.PANEL_SPACING, excAddBtn, excEditBtn, excRemoveBtn),
                exclusionList,
                incLabel,
                new HBox(UIConstants.PANEL_SPACING, incAddBtn, incEditBtn, incRemoveBtn),
                inclusionList);
        content.setPadding(UIConstants.PANEL_INSETS);
        setContent(content);
        setCollapsible(true);
        setExpanded(false);
        setPrefWidth(UIConstants.RULES_PANEL_WIDTH);
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        boolean noSchedule = schedule == null;
        excAddBtn.setDisable(noSchedule);
        excEditBtn.setDisable(true);
        excRemoveBtn.setDisable(true);
        incAddBtn.setDisable(noSchedule);
        incEditBtn.setDisable(true);
        incRemoveBtn.setDisable(true);
        refreshList();
    }

    public void refresh() {
        refreshList();
    }

    private void refreshList() {
        exclusionList.getItems().setAll(schedule == null ? java.util.List.of() : schedule.exclusionGroups());
        inclusionList.getItems().setAll(schedule == null ? java.util.List.of() : schedule.inclusionGroups());
    }

    private void addExclusionGroup() {
        if (schedule == null || schedule.roster().size() < 2) {
            new Alert(Alert.AlertType.WARNING, "Need at least 2 clinicians to create a group.")
                    .showAndWait();
            return;
        }
        GroupDialog dialog = new GroupDialog(schedule.roster(), GroupDialog.GroupType.EXCLUSION, null, null);
        dialog.showAndWait().ifPresent(result -> {
            try {
                schedule.addExclusionGroup(new ExclusionGroup(result.name(), result.members()));
                refreshList();
                onRulesChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void editExclusionGroup() {
        ExclusionGroup selected = exclusionList.getSelectionModel().getSelectedItem();
        if (schedule == null || selected == null) return;
        GroupDialog dialog = new GroupDialog(schedule.roster(), GroupDialog.GroupType.EXCLUSION,
                selected.name(), selected.members());
        dialog.showAndWait().ifPresent(result -> {
            try {
                schedule.replaceExclusionGroup(selected.name(),
                        new ExclusionGroup(result.name(), result.members()));
                refreshList();
                onRulesChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void removeExclusionGroup() {
        ExclusionGroup selected = exclusionList.getSelectionModel().getSelectedItem();
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

    private void addInclusionGroup() {
        if (schedule == null || schedule.roster().size() < 2) {
            new Alert(Alert.AlertType.WARNING, "Need at least 2 clinicians to create a group.")
                    .showAndWait();
            return;
        }
        GroupDialog dialog = new GroupDialog(schedule.roster(), GroupDialog.GroupType.INCLUSION, null, null);
        dialog.showAndWait().ifPresent(result -> {
            try {
                schedule.addInclusionGroup(new InclusionGroup(result.name(), result.members()));
                refreshList();
                onRulesChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void editInclusionGroup() {
        InclusionGroup selected = inclusionList.getSelectionModel().getSelectedItem();
        if (schedule == null || selected == null) return;
        GroupDialog dialog = new GroupDialog(schedule.roster(), GroupDialog.GroupType.INCLUSION,
                selected.name(), selected.members());
        dialog.showAndWait().ifPresent(result -> {
            try {
                schedule.replaceInclusionGroup(selected.name(),
                        new InclusionGroup(result.name(), result.members()));
                refreshList();
                onRulesChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void removeInclusionGroup() {
        InclusionGroup selected = inclusionList.getSelectionModel().getSelectedItem();
        if (schedule == null || selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove inclusion group '" + selected.name() + "'?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                schedule.removeInclusionGroup(selected.name());
                refreshList();
                onRulesChanged.run();
            }
        });
    }
}

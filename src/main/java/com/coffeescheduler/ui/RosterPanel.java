package com.coffeescheduler.ui;

import com.coffeescheduler.model.Clinician;
import com.coffeescheduler.model.Schedule;
import com.coffeescheduler.model.WeekState;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class RosterPanel extends TitledPane {

    private Schedule schedule;
    private final ListView<Clinician> list = new ListView<>();
    private final Button addBtn = new Button("Add clinician");
    private final Button removeBtn = new Button("Remove selected");
    private final Runnable onRosterChanged;
    private final Consumer<Clinician> onClinicianSelected;

    public RosterPanel(Runnable onRosterChanged, Consumer<Clinician> onClinicianSelected) {
        super("Roster", null);
        this.onRosterChanged = onRosterChanged;
        this.onClinicianSelected = onClinicianSelected;

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Clinician item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int scheduled = countOn(item);
                    setText(item.name() + " — " + scheduled + "/" + item.contractedWeeks().max());
                }
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);

        addBtn.setOnAction(e -> addClinician());
        removeBtn.setOnAction(e -> removeClinician());
        addBtn.setDisable(true);
        removeBtn.setDisable(true);

        list.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            removeBtn.setDisable(sel == null);
            if (sel != null) {
                onClinicianSelected.accept(sel);
            }
        });

        VBox content = new VBox(8, new HBox(8, addBtn, removeBtn), list);
        content.setPadding(new Insets(8));
        setContent(content);
        setCollapsible(true);
        setPrefWidth(220);
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        addBtn.setDisable(schedule == null);
        removeBtn.setDisable(true);
        refreshList();
    }

    public Clinician getSelectedClinician() {
        return list.getSelectionModel().getSelectedItem();
    }

    private void addClinician() {
        if (schedule == null) return;
        AddClinicianDialog.prompt().ifPresent(clinician -> {
            try {
                schedule.addClinician(clinician);
                refreshList();
                onRosterChanged.run();
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });
    }

    private void removeClinician() {
        Clinician selected = list.getSelectionModel().getSelectedItem();
        if (schedule == null || selected == null) return;
        schedule.removeClinician(selected);
        refreshList();
        onRosterChanged.run();
    }

    public void refresh() {
        refreshList();
    }

    private void refreshList() {
        list.getItems().setAll(schedule == null ? java.util.List.of() : schedule.roster());
    }

    private int countOn(Clinician c) {
        if (schedule == null) return 0;
        int count = 0;
        for (int w = 1; w <= schedule.lengthWeeks(); w++) {
            if (schedule.stateOf(c, w) == WeekState.ON) count++;
        }
        return count;
    }
}

package com.coffeescheduler.ui;

import com.coffeescheduler.model.RuleViolation;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ViolationsPanel extends VBox {

    private final VBox rows = new VBox(2);
    private final Consumer<RuleViolation> onJump;

    public ViolationsPanel(Consumer<RuleViolation> onJump) {
        this.onJump = onJump;

        Label title = new Label("Violations");
        title.setStyle("-fx-font-weight: bold;");

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(120);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        setPadding(new Insets(4, 8, 4, 8));
        setStyle("-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        getChildren().addAll(title, scroll);
    }

    public void setViolations(List<RuleViolation> violations) {
        rows.getChildren().clear();
        if (violations.isEmpty()) {
            rows.getChildren().add(new Label("No violations."));
            return;
        }
        for (RuleViolation v : violations) {
            rows.getChildren().add(buildRow(v));
        }
    }

    private HBox buildRow(RuleViolation v) {
        Label message = new Label(v.message());
        message.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(message, Priority.ALWAYS);

        Label jump = new Label("Jump");
        jump.setStyle("-fx-text-fill: #1976D2; -fx-underline: true;");
        jump.setCursor(Cursor.HAND);
        jump.setOnMouseClicked(e -> onJump.accept(v));

        HBox row = new HBox(8, message, jump);
        row.setPadding(new Insets(2, 4, 2, 4));
        return row;
    }
}

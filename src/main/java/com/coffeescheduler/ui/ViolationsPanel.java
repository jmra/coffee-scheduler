package com.coffeescheduler.ui;

import com.coffeescheduler.model.RuleViolation;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ViolationsPanel extends VBox {

    private final VBox rows = new VBox();
    private final Consumer<RuleViolation> onJump;

    public ViolationsPanel(Consumer<RuleViolation> onJump) {
        this.onJump = onJump;

        Label title = new Label("Violations");
        title.setStyle("-fx-font-weight: bold;");

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(UIConstants.VIOLATIONS_SCROLL_HEIGHT);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        setPadding(UIConstants.STATUS_BAR_INSETS);
        setStyle("-fx-border-color: " + UIConstants.COLOR_BORDER + "; -fx-border-width: 1 0 0 0;");
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
        jump.setStyle("-fx-text-fill: " + UIConstants.COLOR_LINK + "; -fx-underline: true;");
        jump.setCursor(Cursor.HAND);
        jump.setOnMouseClicked(e -> onJump.accept(v));

        HBox row = new HBox(UIConstants.VIOLATIONS_ROW_SPACING, message, jump);
        row.setPadding(UIConstants.VIOLATIONS_ROW_INSETS);
        return row;
    }
}

package com.coffeescheduler.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow, UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
        stage.setTitle("Coffee Scheduler");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            e.consume();
            mainWindow.requestClose();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

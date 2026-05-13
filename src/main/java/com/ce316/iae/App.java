package com.ce316.iae;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX entry point. Real UI shell is owned by the UI & Deployment module —
 * this is a stub so the application plugin has a main class and so we can
 * verify the build end-to-end.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Integrated Assignment Environment");
        stage.setScene(new Scene(new StackPane(
                new Label("IAE — UI shell pending (owned by UI & Deployment)")), 480, 240));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

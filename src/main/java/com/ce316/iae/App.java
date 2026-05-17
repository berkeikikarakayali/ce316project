package com.ce316.iae;

import com.ce316.iae.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

/**
 * JavaFX entry point — loads the main shell ({@link MainController}).
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL url = Objects.requireNonNull(App.class.getResource("/fxml/MainView.fxml"),
                "FXML bundle missing: /fxml/MainView.fxml");
        FXMLLoader loader = new FXMLLoader(url);
        Scene scene = new Scene(loader.load(), 980, 680);
        MainController controller = loader.getController();
        controller.setPrimaryStage(stage);

        stage.setTitle("Integrated Assignment Environment");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package org.brabocoin.brabocoin.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.brabocoin.brabocoin.gui.view.MainView;

/**
 * Brabocoin GUI application.
 */
public class BrabocoinGUI extends Application {

    private static final String VERSION = BrabocoinGUI.class.getPackage()
        .getImplementationVersion() != null ? BrabocoinGUI.class.getPackage()
        .getImplementationVersion() : "[development version]";

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView);

        // Add base stylesheet
        scene.getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());

        primaryStage.setTitle("Brabocoin " + VERSION);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

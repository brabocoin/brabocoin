package org.brabocoin.brabocoin.gui;

import com.google.protobuf.ByteString;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.brabocoin.brabocoin.BrabocoinApplication;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.view.MainView;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.fxmisc.cssfx.CSSFX;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Brabocoin GUI application.
 */
public class BrabocoinGUI extends Application {

    private static final String VERSION = BrabocoinGUI.class.getPackage()
        .getImplementationVersion() != null ? BrabocoinGUI.class.getPackage()
        .getImplementationVersion() : "[development version]";

    private static final List<String> ICONS = Arrays.asList(
        "icon/16.png",
        "icon/24.png",
        "icon/32.png",
        "icon/40.png",
        "icon/48.png"
    );

    @Override
    public void start(Stage stage) throws DatabaseException, IOException {
        BrabocoinApplication application = new BrabocoinApplication();

        MainView mainView = new MainView(application.getState());
        Scene scene = new Scene(mainView);

        // Auto CSS reloading
        CSSFX.addConverter(uri -> Paths.get(uri.startsWith("file:/") ? uri.replace("file:/", "")
            .replace("out/production/", "src/main/") : uri)).start();

        // Add base stylesheet
        scene.getStylesheets().add(getClass().getResource("brabocoin.css").toExternalForm());

        stage.getIcons()
            .addAll(ICONS.stream()
                .map(path -> new Image(BrabocoinGUI.class.getResourceAsStream(path)))
                .collect(Collectors.toList()));

        stage.setTitle("Brabocoin " + VERSION);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            try {
                application.start();
            }
            catch (IOException | DatabaseException e) {
                Platform.exit();
            }
        });

        /**
         * TODO: Remove me!
         */
        Block block = new Block(
            new Hash(ByteString.copyFromUtf8("prev")),
            new Hash(ByteString.copyFromUtf8("merkle")),
            new Hash(ByteString.copyFromUtf8("target")),
            BigInteger.valueOf(1234),
            10,
            Collections.emptyList(),
            1
        );
        Dialog dialog = new ValidationWindow(application.getState().getBlockchain(), block);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

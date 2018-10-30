package org.brabocoin.brabocoin.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Sten Wessel
 */
public class BraboControlInitializer {

    private static final Logger LOGGER = Logger.getLogger(BraboControlInitializer.class.getName());

    public static <C extends BraboControl> void initialize(C control) {
        FXMLLoader fxmlLoader = new FXMLLoader(control.getClass().getResource(control.fxmlFilePath()));
        fxmlLoader.setControllerFactory(a -> control);
        fxmlLoader.setRoot(control);

        Parent root;
        try {
            root = fxmlLoader.load();
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "GUI component could not be initialized", e);
            throw new RuntimeException(e);
        }

        root.getStylesheets()
            .addAll(control.stylesheets().stream()
                .map(s -> control.getClass().getResource(s))
                .filter(Objects::nonNull)
                .map(URL::toExternalForm)
                .collect(Collectors.toList()));
    }
}

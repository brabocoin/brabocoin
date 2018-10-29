package org.brabocoin.brabocoin.gui;

import com.google.common.base.CaseFormat;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Base control class for all Brabocoin GUI controls.
 * <p>
 * Automatically binds and loads the corresponding FXML and CSS style sheets. When the control
 * class is named {@code CustomControl}, the loaded FXML is {@code custom_control.fxml} by
 * default, located in the resource folder in the same package as the {@code CustomControl} class.
 * By default, {@code custom_control.css} is applied to the root element, if the stylesheet
 * exists.
 */
public abstract class BraboControl {

    private static final Logger LOGGER = Logger.getLogger(BraboControl.class.getName());

    /**
     * Root element from the FXML.
     */
    private final @NotNull Parent root;

    /**
     * Create the control element by loading the FXML and binding stylesheets.
     *
     * @throws RuntimeException
     *     When the FXML could not be loaded.
     */
    public BraboControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilePath()));
        fxmlLoader.setController(this);

        try {
            root = fxmlLoader.load();
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "GUI component could not be initialized", e);
            throw new RuntimeException(e);
        }

        root.getStylesheets()
            .addAll(stylesheets().stream()
                .map(s -> getClass().getResource(s))
                .filter(Objects::nonNull)
                .map(URL::toExternalForm)
                .collect(Collectors.toList()));
    }

    /**
     * The path relative to the current class to the FXML file.
     *
     * @return The path to the FXML file.
     */
    protected @NotNull String fxmlFilePath() {
        return resourceName() + ".fxml";
    }

    /**
     * Paths relative to the current class to stylesheets loaded for the root.
     *
     * @return The paths to the stylesheets.
     */
    protected @NotNull Collection<String> stylesheets() {
        return Collections.singletonList(resourceName() + ".css");
    }

    /**
     * The root element for the control as loaded from the FXML.
     *
     * @return The root element.
     */
    public @NotNull Parent getRoot() {
        return root;
    }

    private @NotNull String resourceName() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName());
    }
}

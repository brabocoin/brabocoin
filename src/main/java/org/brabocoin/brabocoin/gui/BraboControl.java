package org.brabocoin.brabocoin.gui;

import com.google.common.base.CaseFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Base control class for all Brabocoin GUI controls.
 * <p>
 * Automatically binds and loads the corresponding FXML and CSS style sheets. When the control
 * class is named {@code CustomControl}, the loaded FXML is {@code custom_control.fxml} by
 * default, located in the resource folder in the same package as the {@code CustomControl} class.
 * By default, {@code custom_control.css} is applied to the root element, if the stylesheet
 * exists.
 */
public interface BraboControl {

    /**
     * The path relative to the current class to the FXML file.
     *
     * @return The path to the FXML file.
     */
    default  @NotNull String fxmlFilePath() {
        return resourceName() + ".fxml";
    }

    /**
     * Paths relative to the current class to stylesheets loaded for the root.
     *
     * @return The paths to the stylesheets.
     */
    default  @NotNull Collection<String> stylesheets() {
        return Collections.singletonList(resourceName() + ".css");
    }

    /**
     * Gets the associated resource name for this control, without file extension.
     *
     * @return The base resource name.
     */
    default @NotNull String resourceName() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName());
    }
}

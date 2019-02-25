package org.brabocoin.brabocoin.config.annotation;

import com.dlsc.preferencesfx.model.Setting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

/**
 * Used to annotate PreferencesFX settings in a Cfg4J config interface.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BraboPref {

    /**
     * The name of this setting.
     */
    String name();

    /**
     * The destination {@link BraboPrefCategory} or {@link BraboPrefGroup} class.
     */
    Class destination();

    /**
     * Items will be sorted according to this integer, lower equals higher priority.
     */
    int order() default 0;
}

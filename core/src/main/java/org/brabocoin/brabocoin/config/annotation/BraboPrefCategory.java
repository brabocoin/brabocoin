package org.brabocoin.brabocoin.config.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to annotate PreferencesFX categories in a Preference Tree
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BraboPrefCategory {

    /**
     * The name of this category.
     */
    String name();

    /**
     * Items will be sorted according to this integer, lower equals higher priority.
     */
    int order() default 0;
}

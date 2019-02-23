package org.brabocoin.brabocoin.config.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to annotate PreferencesFX groups in a Preference Tree
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BraboPrefGroup {

    /**
     * The name of this group.
     */
    String name() default "";

    /**
     * Items will be sorted according to this integer, lower equals higher priority.
     */
    int order() default 0;
}

package org.brabocoin.brabocoin.config.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BraboPrefGroup {
    String name() default "";
}

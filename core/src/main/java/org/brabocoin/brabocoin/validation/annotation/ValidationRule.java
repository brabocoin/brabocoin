package org.brabocoin.brabocoin.validation.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ValidationRule {
    String name();
    String description() default "";
    boolean composite() default false;
}

package org.brabocoin.brabocoin.config.annotation;

import com.dlsc.preferencesfx.model.Setting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

@Retention(RetentionPolicy.RUNTIME)
public @interface BraboPref {
    String name();
    Class destination();
    int order() default 0;
}

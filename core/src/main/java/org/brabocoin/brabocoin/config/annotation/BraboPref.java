package org.brabocoin.brabocoin.config.annotation;

import com.dlsc.preferencesfx.model.Setting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

@Retention(RetentionPolicy.RUNTIME)
public @interface BraboPref {
    String name();
    Class group();
    int min() default Integer.MIN_VALUE;
    int max() default Integer.MAX_VALUE;
    int precision() default Integer.MIN_VALUE;
}

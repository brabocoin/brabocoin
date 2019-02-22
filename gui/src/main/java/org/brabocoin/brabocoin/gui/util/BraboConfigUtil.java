package org.brabocoin.brabocoin.gui.util;

import org.brabocoin.brabocoin.config.BraboConfig;

import java.lang.reflect.Method;

public class BraboConfigUtil {
    public void getConfigPreferences(BraboConfig config) {
        for (Method method : BraboConfig.class.getDeclaredMethods()) {
            method.getReturnType();
        }
    }
}

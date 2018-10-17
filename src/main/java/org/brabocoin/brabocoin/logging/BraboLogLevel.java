package org.brabocoin.brabocoin.logging;

import java.util.logging.Level;

public class BraboLogLevel extends Level {
    public static final Level EDU = new BraboLogLevel("EDUCATIONAL", Level.INFO.intValue() - 1);

    protected BraboLogLevel(String name, int value) {
        super(name, value);
    }
}

package org.brabocoin.brabocoin.util;

import javafx.util.Pair;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.validation.Consensus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

class ConfigUtilTest {

    @Test
    void write() throws IOException, InvocationTargetException, IllegalAccessException {
        BraboConfig config = new BraboConfig();
        Consensus consensus = new Consensus();
        ConfigUtil.write(config, consensus, new File("dump"));
        Pair<BraboConfig, Consensus> readPair = ConfigUtil.read(new File(""
            + "ump"));

        Assertions.assertNotNull(readPair);
    }
}
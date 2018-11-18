package org.brabocoin.brabocoin.node.config;

import org.cfg4j.provider.ConfigurationProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BraboConfigProviderTest {

    @Test
    void getConfig() {
        ConfigurationProvider config = BraboConfigProvider.getConfig();
        assertNotNull(config);
    }

    @Test
    void getBraboConfig() {
        ConfigurationProvider config = BraboConfigProvider.getConfig();
        BraboConfig braboConfig = config.bind("brabo", BraboConfig.class);
        assertNotNull(braboConfig);
    }
}
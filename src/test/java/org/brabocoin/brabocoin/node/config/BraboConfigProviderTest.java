package org.brabocoin.brabocoin.node.config;

import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BraboConfigProviderTest {

    class BraboConfigProviderMock extends BraboConfigProvider {
        @Override
        public String getConfigDirectory() {
            return "./src/test/resources";
        }
    }

    @Test
    void instantiationTest() throws UnknownHostException {
        BraboConfigProvider provider = new BraboConfigProviderMock();
        BraboConfig config = provider.getConfig().bind("brabo", BraboConfig.class);

        assertTrue(config.bootstrapPeers().get(0).startsWith("localhost"));
    }
}
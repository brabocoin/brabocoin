package org.brabocoin.brabocoin.services;

import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.MockNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeTest {
    BraboConfig defaultConfig = new BraboConfigProvider().getConfig().bind("brabo", BraboConfig.class);

    @Test
    void handshakeTest(TestInfo testInfo) throws IOException {
        Node peerA = new MockNode(8091, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        });
        Node peerB = new MockNode(8092, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<>();
            }
        });

        Node responder = new MockNode(8090, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8091");
                    add("localhost:8092");
                }};
            }
        });
        Node greeter = new MockNode(8089, new MockBraboConfig(defaultConfig) {
            @Override
            public List<String> bootstrapPeers() {
                return new ArrayList<String>() {{
                    add("localhost:8090");
                }};
            }
        });

        peerA.start();
        peerB.start();

        responder.start();
        greeter.start();

        assertEquals(3, greeter.environment.getPeers().size());

        peerA.stop();
        peerB.stop();

        responder.stop();
        greeter.stop();
    }
}
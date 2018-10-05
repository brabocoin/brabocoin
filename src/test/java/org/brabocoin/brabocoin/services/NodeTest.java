package org.brabocoin.brabocoin.services;

import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeTest {
    class MockNode extends Node {
        private String mockConfigFile;
        MockNode(int listenPort, String mockConfigFile) {
            super(listenPort);
            this.mockConfigFile = mockConfigFile + ".yaml";
        }

        @Override
        NodeEnvironment createEnvironment() {
            return new MockEnvironment(mockConfigFile);
        }
    }

    class MockEnvironment extends NodeEnvironment {
        private String mockConfigFile;
        MockEnvironment(String mockConfigFile) {
            super(false);
            this.mockConfigFile = mockConfigFile;
            setup();
        }

        @Override
        public BraboConfigProvider buildConfigProvider() {
            return new BraboConfigProvider(mockConfigFile, "./src/test/resources/");
        }

    }

    @Test
    @DisplayName("handshakeTest")
    void handshakeTest(TestInfo testInfo) throws IOException {
        Node peerA = new MockNode(8091, "emptyBootstrap");
        Node peerB = new MockNode(8092, "emptyBootstrap");

        Node responder = new MockNode(8090, testInfo.getDisplayName() + "Responder");
        Node greeter = new MockNode(8089, testInfo.getDisplayName()  + "Greeter");

        peerA.start();
        peerB.start();

        responder.start();
        greeter.start();

        assertEquals(3, greeter.environment.getPeers().size());
    }
}
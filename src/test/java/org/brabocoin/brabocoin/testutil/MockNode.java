package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.services.Node;

public class MockNode extends Node {
    BraboConfig mockConfig;

    public MockNode(int listenPort, BraboConfig mockConfig) {
        super(listenPort);
        this.mockConfig = mockConfig;
    }

    @Override
    public NodeEnvironment createEnvironment() {
        return new MockEnvironment(mockConfig);
    }
}
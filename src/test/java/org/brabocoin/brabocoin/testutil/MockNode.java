package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.services.Node;

public class MockNode extends Node {
    NodeEnvironment mockEnvironment;

    public MockNode(int listenPort, NodeEnvironment mockEnvironment) {
        super(listenPort);
        this.mockEnvironment = mockEnvironment;
    }

    @Override
    protected NodeEnvironment createEnvironment() {
        return mockEnvironment;
    }
}
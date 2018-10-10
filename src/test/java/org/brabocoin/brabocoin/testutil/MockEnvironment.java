package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;

class MockEnvironment extends NodeEnvironment {
    MockEnvironment(BraboConfig mockConfig) {
        super();
        this.config = mockConfig;
    }
}
package org.brabocoin.brabocoin.node;

import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.junit.jupiter.api.Test;

class PeerTest {
    @Test
    void fakePeerInstantiation() throws MalformedSocketException {
        Peer p = new Peer("hatseflats:666");
    }
}
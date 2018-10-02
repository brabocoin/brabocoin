package org.brabocoin.brabocoin.node;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a node environment.
 */
public class Environment {
    private Set<Peer> peers = new HashSet<>();

    public void addPeer(Peer peer) {
        peers.add(peer);
    }
}

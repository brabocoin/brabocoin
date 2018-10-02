package org.brabocoin.brabocoin.node;

import java.net.InetAddress;

/**
 * A representation of an peer in the network.
 */
public class Peer {
    private InetAddress address;
    public Peer(InetAddress address) {
        this.address = address;
    }
}

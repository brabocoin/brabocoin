package org.brabocoin.brabocoin.listeners;

import org.brabocoin.brabocoin.node.Peer;

public interface PeerSetChangedListener {
    void onPeerAdded(Peer peer);

    void onPeerRemoved(Peer peer);
}

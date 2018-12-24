package org.brabocoin.brabocoin.node;

public interface NetworkMessageListener {

    default void onReceivedMessage(NetworkMessage message) {
    }

    default void onSendMessage(NetworkMessage message) {
    }
}

package org.brabocoin.brabocoin.node;

public interface NetworkMessageListener {

    default void onIncomingMessage(NetworkMessage message, boolean isUpdate) {
    }

    default void onOutgoingMessage(NetworkMessage message, boolean isUpdate) {
    }
}

package org.brabocoin.brabocoin.listeners;

public interface ReorganizeChainListener {
    void onStartOrganization();

    void onFinishOrganization();
}

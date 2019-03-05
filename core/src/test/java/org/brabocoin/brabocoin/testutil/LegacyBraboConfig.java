package org.brabocoin.brabocoin.testutil;

import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.config.MutableBraboConfig;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class LegacyBraboConfig {

    private BraboConfig delegator;

    public void setDelegator(BraboConfig delegator) {
        this.delegator = delegator;
    }

    public LegacyBraboConfig(BraboConfig delegator) {
        this.delegator = delegator;
    }

    public Integer networkId() {
        return this.delegator.getNetworkId();
    }


    public Integer loopInterval() {
        return delegator.getLoopInterval();
    }


    public Integer targetPeerCount() {
        return delegator.getTargetPeerCount();
    }


    public Integer handshakeDeadline() {
        return delegator.getHandshakeDeadline();
    }


    public List<String> bootstrapPeers() {
        return delegator.getBootstrapPeers();
    }


    public String dataDirectory() {
        return "src/test/resources/" + delegator.getDataDirectory();
    }


    public Integer updatePeerInterval() {
        return delegator.getUpdatePeerInterval();
    }


    public String databaseDirectory() {
        return delegator.getDatabaseDirectory();
    }


    public String blockStoreDirectory() {
        return delegator.getBlockStoreDirectory();
    }


    public String utxoStoreDirectory() {
        return delegator.getUtxoStoreDirectory();
    }


    public String walletStoreDirectory() {
        return delegator.getWalletStoreDirectory();
    }


    public Integer maxBlockFileSize() {
        return delegator.getMaxBlockFileSize();
    }


    public Integer maxTransactionPoolSize() {
        return delegator.getMaxTransactionPoolSize();
    }


    public Integer maxOrphanTransactions() {
        return delegator.getMaxOrphanTransactions();
    }


    public Integer maxOrphanBlocks() {
        return delegator.getMaxOrphanBlocks();
    }


    public Integer maxRecentRejectBlocks() {
        return delegator.getMaxRecentRejectBlocks();
    }


    public Integer maxRecentRejectTransactions() {
        return delegator.getMaxRecentRejectTransactions();
    }


    public Integer servicePort() {
        return delegator.getServicePort();
    }


    public String walletFile() {
        return delegator.getWalletFile();
    }


    public String transactionHistoryFile() {
        return delegator.getTransactionHistoryFile();
    }


    public Integer maxSequentialOrphanBlocks() {
        return delegator.getMaxSequentialOrphanBlocks();
    }

    public Boolean allowLocalPeers() {
        return delegator.isAllowLocalPeers();
    }

    public BraboConfig toBraboConfig() {
        BraboConfig config = new MutableBraboConfig();
        for (Field f : config.getClass().getFields()) {
            Property property;
            try {
                property = (Property)f.get(config);

            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }

            // Find matching method
            for (Method m : this.getClass().getMethods()) {
                if (m.getName().equals(f.getName())) {
                    m.setAccessible(true);
                    try {
                        if (m.getReturnType().equals(List.class)) {
                            property.setValue(FXCollections.observableArrayList((List<String>)m.invoke(
                                this)));
                            break;
                        }
                        property.setValue(m.invoke(this));
                        break;
                    }
                    catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return config;
    }
}

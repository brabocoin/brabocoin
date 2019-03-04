package org.brabocoin.brabocoin.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;

/**
 * The configuration class for Brabocoin
 */
public class BraboConfig {
    public IntegerProperty networkId = new SimpleIntegerProperty(1);

    public IntegerProperty servicePort = new SimpleIntegerProperty(56129);

    public IntegerProperty targetPeerCount = new SimpleIntegerProperty(25);

    public IntegerProperty updatePeerInterval = new SimpleIntegerProperty(45);

    public BooleanProperty allowLocalPeers = new SimpleBooleanProperty(false);

    public IntegerProperty maxSequentialOrphanBlocks = new SimpleIntegerProperty(10);

    public IntegerProperty loopInterval = new SimpleIntegerProperty(500);

    public IntegerProperty handshakeDeadline = new SimpleIntegerProperty(2000);

    // Note: lists do not have a PreferencesFX control
    public ListProperty<String> bootstrapPeers =
        new SimpleListProperty<>(FXCollections.observableArrayList(
            "brabocoin.org:56129"));

    public StringProperty dataDirectory = new SimpleStringProperty("data");

    public StringProperty databaseDirectory = new SimpleStringProperty("db");

    public StringProperty blockStoreDirectory = new SimpleStringProperty("blocks");

    public StringProperty utxoStoreDirectory = new SimpleStringProperty("utxo");

    public StringProperty walletStoreDirectory = new SimpleStringProperty("wallet");

    public StringProperty walletFile = new SimpleStringProperty("wallet.dat");

    public StringProperty transactionHistoryFile = new SimpleStringProperty("txhist.dat");

    public IntegerProperty maxBlockFileSize = new SimpleIntegerProperty(128000000);

    public IntegerProperty maxOrphanBlocks = new SimpleIntegerProperty(100);

    public IntegerProperty maxRecentRejectBlocks = new SimpleIntegerProperty(20);

    public IntegerProperty maxTransactionPoolSize = new SimpleIntegerProperty(300);

    public IntegerProperty maxOrphanTransactions = new SimpleIntegerProperty(100);

    public IntegerProperty maxRecentRejectTransactions = new SimpleIntegerProperty(20);

    public int getNetworkId() {
        return networkId.get();
    }

    public int getServicePort() {
        return servicePort.get();
    }

    public int getTargetPeerCount() {
        return targetPeerCount.get();
    }

    public int getUpdatePeerInterval() {
        return updatePeerInterval.get();
    }

    public boolean isAllowLocalPeers() {
        return allowLocalPeers.get();
    }

    public int getMaxSequentialOrphanBlocks() {
        return maxSequentialOrphanBlocks.get();
    }

    public int getLoopInterval() {
        return loopInterval.get();
    }

    public int getHandshakeDeadline() {
        return handshakeDeadline.get();
    }

    public List<String> getBootstrapPeers() {
        return new ArrayList<>(bootstrapPeers.get());
    }

    public String getDataDirectory() {
        return dataDirectory.get();
    }

    public String getDatabaseDirectory() {
        return databaseDirectory.get();
    }

    public String getBlockStoreDirectory() {
        return blockStoreDirectory.get();
    }

    public String getUtxoStoreDirectory() {
        return utxoStoreDirectory.get();
    }

    public String getWalletStoreDirectory() {
        return walletStoreDirectory.get();
    }

    public String getWalletFile() {
        return walletFile.get();
    }

    public String getTransactionHistoryFile() {
        return transactionHistoryFile.get();
    }

    public int getMaxBlockFileSize() {
        return maxBlockFileSize.get();
    }

    public int getMaxOrphanBlocks() {
        return maxOrphanBlocks.get();
    }

    public int getMaxRecentRejectBlocks() {
        return maxRecentRejectBlocks.get();
    }

    public int getMaxTransactionPoolSize() {
        return maxTransactionPoolSize.get();
    }

    public int getMaxOrphanTransactions() {
        return maxOrphanTransactions.get();
    }

    public int getMaxRecentRejectTransactions() {
        return maxRecentRejectTransactions.get();
    }

}

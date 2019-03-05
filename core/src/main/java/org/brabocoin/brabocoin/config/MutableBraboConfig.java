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

public class MutableBraboConfig implements BraboConfig {

    public MutableBraboConfig() {
        initializeDefaultValues();
    }

    public void initializeDefaultValues() {
        networkId.setValue(1);
        servicePort.setValue(56129);
        targetPeerCount.setValue(25);
        updatePeerInterval.setValue(45);
        allowLocalPeers.setValue(false);
        maxSequentialOrphanBlocks.setValue(10);
        loopInterval.setValue(500);
        handshakeDeadline.setValue(2000);
        bootstrapPeers.setValue(FXCollections.observableArrayList(
            "brabocoin.org:56129"));
        dataDirectory.setValue("data");
        databaseDirectory.setValue("db");
        blockStoreDirectory.setValue("blocks");
        utxoStoreDirectory.setValue("utxo");
        walletStoreDirectory.setValue("wallet");
        walletFile.setValue("wallet.dat");
        transactionHistoryFile.setValue("txhist.dat");
        maxBlockFileSize.setValue(128000000);
        maxOrphanBlocks.setValue(100);
        maxRecentRejectBlocks.setValue(20);
        maxTransactionPoolSize.setValue(300);
        maxOrphanTransactions.setValue(100);
        maxRecentRejectTransactions.setValue(20);
    }

    public IntegerProperty networkId = new SimpleIntegerProperty();

    public IntegerProperty servicePort = new SimpleIntegerProperty();

    public IntegerProperty targetPeerCount = new SimpleIntegerProperty();

    public IntegerProperty updatePeerInterval = new SimpleIntegerProperty();

    public BooleanProperty allowLocalPeers = new SimpleBooleanProperty();

    public IntegerProperty maxSequentialOrphanBlocks = new SimpleIntegerProperty();

    public IntegerProperty loopInterval = new SimpleIntegerProperty();

    public IntegerProperty handshakeDeadline = new SimpleIntegerProperty();

    // Note: lists do not have a PreferencesFX control
    public ListProperty<String> bootstrapPeers = new SimpleListProperty<>();

    public StringProperty dataDirectory = new SimpleStringProperty();

    public StringProperty databaseDirectory = new SimpleStringProperty();

    public StringProperty blockStoreDirectory = new SimpleStringProperty();

    public StringProperty utxoStoreDirectory = new SimpleStringProperty();

    public StringProperty walletStoreDirectory = new SimpleStringProperty();

    public StringProperty walletFile = new SimpleStringProperty();

    public StringProperty transactionHistoryFile = new SimpleStringProperty();

    public IntegerProperty maxBlockFileSize = new SimpleIntegerProperty();

    public IntegerProperty maxOrphanBlocks = new SimpleIntegerProperty();

    public IntegerProperty maxRecentRejectBlocks = new SimpleIntegerProperty();

    public IntegerProperty maxTransactionPoolSize = new SimpleIntegerProperty();

    public IntegerProperty maxOrphanTransactions = new SimpleIntegerProperty();

    public IntegerProperty maxRecentRejectTransactions = new SimpleIntegerProperty();

    @Override
    public int getNetworkId() {
        return networkId.get();
    }

    @Override
    public int getServicePort() {
        return servicePort.get();
    }

    @Override
    public int getTargetPeerCount() {
        return targetPeerCount.get();
    }

    @Override
    public int getUpdatePeerInterval() {
        return updatePeerInterval.get();
    }

    @Override
    public boolean isAllowLocalPeers() {
        return allowLocalPeers.get();
    }

    @Override
    public int getMaxSequentialOrphanBlocks() {
        return maxSequentialOrphanBlocks.get();
    }

    @Override
    public int getLoopInterval() {
        return loopInterval.get();
    }

    @Override
    public int getHandshakeDeadline() {
        return handshakeDeadline.get();
    }

    @Override
    public List<String> getBootstrapPeers() {
        return new ArrayList<>(bootstrapPeers.get());
    }

    @Override
    public String getDataDirectory() {
        return dataDirectory.get();
    }

    @Override
    public String getDatabaseDirectory() {
        return databaseDirectory.get();
    }

    @Override
    public String getBlockStoreDirectory() {
        return blockStoreDirectory.get();
    }

    @Override
    public String getUtxoStoreDirectory() {
        return utxoStoreDirectory.get();
    }

    @Override
    public String getWalletStoreDirectory() {
        return walletStoreDirectory.get();
    }

    @Override
    public String getWalletFile() {
        return walletFile.get();
    }

    @Override
    public String getTransactionHistoryFile() {
        return transactionHistoryFile.get();
    }

    @Override
    public int getMaxBlockFileSize() {
        return maxBlockFileSize.get();
    }

    @Override
    public int getMaxOrphanBlocks() {
        return maxOrphanBlocks.get();
    }

    @Override
    public int getMaxRecentRejectBlocks() {
        return maxRecentRejectBlocks.get();
    }

    @Override
    public int getMaxTransactionPoolSize() {
        return maxTransactionPoolSize.get();
    }

    @Override
    public int getMaxOrphanTransactions() {
        return maxOrphanTransactions.get();
    }

    @Override
    public int getMaxRecentRejectTransactions() {
        return maxRecentRejectTransactions.get();
    }
}

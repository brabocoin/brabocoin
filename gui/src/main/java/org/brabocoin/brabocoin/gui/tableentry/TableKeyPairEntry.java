package org.brabocoin.brabocoin.gui.tableentry;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.wallet.Wallet;

public class TableKeyPairEntry {

    private final SimpleObjectProperty<PrivateKey> privateKey;

    private final SimpleObjectProperty<PublicKey> publicKey;

    private final SimpleBooleanProperty encrypted;

    private final SimpleObjectProperty<Hash> address;

    private final SimpleLongProperty confirmedBalance;

    private final SimpleLongProperty pendingBalance;

    private SimpleIntegerProperty index;

    private final KeyPair keyPair;

    private final Wallet wallet;

    public TableKeyPairEntry(KeyPair keyPair, Wallet wallet, int index) {
        this.index = new SimpleIntegerProperty(index);
        this.privateKey = new SimpleObjectProperty<>(keyPair.getPrivateKey());
        this.publicKey = new SimpleObjectProperty<>(keyPair.getPublicKey());
        this.encrypted = new SimpleBooleanProperty(keyPair.getPrivateKey().isEncrypted());
        this.address = new SimpleObjectProperty<>(keyPair.getPublicKey().getHash());
        this.confirmedBalance = new SimpleLongProperty();
        this.pendingBalance = new SimpleLongProperty();
        this.wallet = wallet;
        this.keyPair = keyPair;
        updateBalances();
    }

    public TableKeyPairEntry(KeyPair keyPair, Wallet wallet) {
        this(keyPair, wallet, 0);
    }

    public PrivateKey getPrivateKey() {
        return privateKey.get();
    }

    public PublicKey getPublicKey() {
        return publicKey.get();
    }

    public int getIndex() {
        return index.get();
    }

    public boolean getEncrypted() {
        return encrypted.get();
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public Hash getAddress() {
        return address.get();
    }

    public long getConfirmedBalance() {
        return confirmedBalance.get();
    }
    public long getPendingBalance() {
        return pendingBalance.get();
    }

    public void updateBalances() {
        confirmedBalance.set(wallet.computeKeyPairBalance(false, keyPair));
        pendingBalance.set(wallet.computeKeyPairBalance(true, keyPair));
    }
}

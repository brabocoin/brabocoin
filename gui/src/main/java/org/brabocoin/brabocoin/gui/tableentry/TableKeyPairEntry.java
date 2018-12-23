package org.brabocoin.brabocoin.gui.tableentry;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;

public class TableKeyPairEntry {

    private final SimpleObjectProperty<PrivateKey> privateKey;

    private final SimpleObjectProperty<PublicKey> publicKey;

    private final SimpleBooleanProperty encrypted;

    private final SimpleObjectProperty<Hash> address;

    private SimpleIntegerProperty index;

    public TableKeyPairEntry(KeyPair keyPair, int index) {
        this.index = new SimpleIntegerProperty(index);
        this.privateKey = new SimpleObjectProperty<>(keyPair.getPrivateKey());
        this.publicKey = new SimpleObjectProperty<>(keyPair.getPublicKey());
        this.encrypted = new SimpleBooleanProperty(keyPair.getPrivateKey().isEncrypted());
        this.address = new SimpleObjectProperty<>(keyPair.getPublicKey().getHash());
    }

    public TableKeyPairEntry(KeyPair keyPair) {
        this(keyPair, 0);
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
}

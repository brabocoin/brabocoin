package org.brabocoin.brabocoin.gui.tableentry;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;

public class TableKeyPairEntry {

    private final SimpleObjectProperty<PrivateKey> privateKey;

    private final SimpleObjectProperty<PublicKey> publicKey;

    private final SimpleBooleanProperty encrypted;

    private SimpleIntegerProperty index;

    public TableKeyPairEntry(KeyPair keyPair, int index) {
        this.index = new SimpleIntegerProperty(index);
        this.privateKey = new SimpleObjectProperty<>(keyPair.getPrivateKey());
        this.publicKey = new SimpleObjectProperty<>(keyPair.getPublicKey());
        this.encrypted = new SimpleBooleanProperty(keyPair.getPrivateKey().isEncrypted());
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
}

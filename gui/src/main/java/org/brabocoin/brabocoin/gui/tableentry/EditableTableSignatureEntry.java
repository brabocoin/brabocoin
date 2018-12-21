package org.brabocoin.brabocoin.gui.tableentry;

import com.google.protobuf.ByteString;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.crypto.Signature;

import java.math.BigInteger;

public class EditableTableSignatureEntry {

    private SimpleIntegerProperty index;
    private SimpleObjectProperty<Hash> publicKey;
    private SimpleObjectProperty<BigInteger> r;
    private SimpleObjectProperty<BigInteger> s;

    public EditableTableSignatureEntry(Signature signature, int index) {
        this.index = new SimpleIntegerProperty(index);
        this.publicKey = new SimpleObjectProperty<>(
            new Hash(signature.getPublicKey().toCompressed())
        );
        this.r = new SimpleObjectProperty<>(signature.getR());
        this.s = new SimpleObjectProperty<>(signature.getS());
    }

    private EditableTableSignatureEntry(int index) {
        this.index = new SimpleIntegerProperty(index);
        this.publicKey = new SimpleObjectProperty<>(
            new Hash(ByteString.EMPTY)
        );
        this.r = new SimpleObjectProperty<>(BigInteger.ZERO);
        this.s = new SimpleObjectProperty<>(BigInteger.ZERO);
    }

    public static EditableTableSignatureEntry empty(int index) {
        return new EditableTableSignatureEntry(index);
    }

    public Hash getPublicKey() {
        return publicKey.get();
    }

    public void setPublicKey(Hash publicKey) {
        this.publicKey.set(publicKey);
    }

    public BigInteger getR() {
        return r.get();
    }

    public void setR(BigInteger r) {
        this.r.set(r);
    }

    public BigInteger getS() {
        return s.get();
    }

    public void setS(BigInteger s) {
        this.s.set(s);
    }

    public int getIndex() {
        return index.get();
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public Signature toSignature(EllipticCurve curve) {
        return new Signature(
            getR(),
            getS(),
            PublicKey.fromCompressed(
                getPublicKey().getValue(),
                curve
            )
        );
    }
}

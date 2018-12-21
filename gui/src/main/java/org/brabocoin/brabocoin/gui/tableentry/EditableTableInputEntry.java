package org.brabocoin.brabocoin.gui.tableentry;

import com.google.protobuf.ByteString;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;

public class EditableTableInputEntry {

    private SimpleObjectProperty<Hash> referencedTransaction;

    private SimpleObjectProperty<Hash> address;

    private SimpleLongProperty amount;

    private SimpleIntegerProperty referencedOutputIndex;

    private SimpleIntegerProperty index;

    public EditableTableInputEntry(Input input, int index, Hash address, Long amount) {
        this.referencedTransaction = new SimpleObjectProperty<>(input.getReferencedTransaction());
        this.referencedOutputIndex = new SimpleIntegerProperty(input.getReferencedOutputIndex());
        this.address = new SimpleObjectProperty<>(address);
        this.amount = new SimpleLongProperty(amount);
        this.index = new SimpleIntegerProperty(index);
    }

    public EditableTableInputEntry(Input input, int index) {
        this.referencedTransaction = new SimpleObjectProperty<>(input.getReferencedTransaction());
        this.referencedOutputIndex = new SimpleIntegerProperty(input.getReferencedOutputIndex());
        this.address = new SimpleObjectProperty<>(new Hash(ByteString.EMPTY));
        this.amount = new SimpleLongProperty(0);
        this.index = new SimpleIntegerProperty(index);
    }

    public Hash getReferencedTransaction() {
        return referencedTransaction.get();
    }

    public void setReferencedTransaction(Hash referencedTransaction) {
        this.referencedTransaction.set(referencedTransaction);
    }

    public void setReferencedOutputIndex(int referencedOutputIndex) {
        this.referencedOutputIndex.set(referencedOutputIndex);
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public int getReferencedOutputIndex() {
        return referencedOutputIndex.get();
    }

    public int getIndex() {
        return index.get();
    }

    public void setAddress(Hash address) {
        this.address.set(address);
    }

    public Hash getAddress() {
        return address.get();
    }

    public long getAmount() {
        return amount.get();
    }

    public void setAmount(long amount) {
        this.amount.set(amount);
    }

    public Input toInput() {
        return new Input(
            referencedTransaction.get(),
            referencedOutputIndex.get()
        );
    }
}

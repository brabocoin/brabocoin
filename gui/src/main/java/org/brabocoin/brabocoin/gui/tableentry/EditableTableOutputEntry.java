package org.brabocoin.brabocoin.gui.tableentry;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.validation.Consensus;

public class EditableTableOutputEntry {

    private SimpleIntegerProperty index;

    private SimpleObjectProperty<Hash> address;

    private SimpleDoubleProperty amount;

    public EditableTableOutputEntry(Output output, int index) {
        this.index = new SimpleIntegerProperty(index);
        this.address = new SimpleObjectProperty<>(output.getAddress());
        this.amount = new SimpleDoubleProperty(output.getAmount() / (double)Consensus.COIN);
    }

    public int getIndex() {
        return index.get();
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public Hash getAddress() {
        return address.get();
    }

    public void setAddress(Hash address) {
        this.address.set(address);
    }

    public double getAmount() {
        return amount.get();
    }

    public void setAmount(double amount) {
        this.amount.set(amount);
    }

    public Output toOutput() {
        return new Output(
            address.get(),
            (long)(amount.get() * Consensus.COIN)
        );
    }
}

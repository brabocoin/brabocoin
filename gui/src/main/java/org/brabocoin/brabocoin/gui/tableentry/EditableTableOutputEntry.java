package org.brabocoin.brabocoin.gui.tableentry;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;

public class EditableTableOutputEntry {
    private SimpleIntegerProperty index;

    private SimpleObjectProperty<Hash> address;

    private SimpleLongProperty amount;

    public EditableTableOutputEntry(Output output, int index) {
        this.index =  new SimpleIntegerProperty(index);
        this.address = new SimpleObjectProperty<>(output.getAddress());
        this.amount = new SimpleLongProperty(output.getAmount());
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

    public long getAmount() {
        return amount.get();
    }

    public void setAmount(long amount) {
        this.amount.set(amount);
    }
}

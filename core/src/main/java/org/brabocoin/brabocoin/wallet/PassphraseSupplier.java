package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.util.Destructible;

@FunctionalInterface
public interface PassphraseSupplier {
    Destructible<char[]> supplyPassphrase(boolean creation);
}

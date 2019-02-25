package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotation.BraboPrefCategory;
import org.brabocoin.brabocoin.config.annotation.BraboPrefGroup;

/**
 * The PreferenceFX tree for {@link BraboConfig}.
 * <p>
 * Representation of the PreferencesFX tree structure, @see
 * <a href="https://github.com/dlemmermann/PreferencesFX#structure">the PreferencesFX documentation on this.</a>
 * <p>
 * Each category can have either zero or more groups or zero or more settings.
 * Each category can have zero or more subcategories.
 * Each group can have zero or more settings.
 */
abstract class BraboPreferencesTree {

    @BraboPrefCategory(name = "Network", order = 0)
    abstract class NetworkCategory {

        @BraboPrefGroup(name = "General", order = 0)
        abstract class General {

        }

        @BraboPrefGroup(name = "Advanced", order = 1)
        abstract class Advanced {

        }
    }

    @BraboPrefCategory(name = "Consensus", order = 1)
    abstract class Consensus {

    }

    @BraboPrefCategory(name = "Storage", order = 2)
    abstract class StorageCategory {

        @BraboPrefGroup(name = "General", order = 0)
        abstract class General {

        }

        @BraboPrefGroup(name = "Wallet details", order = 3)
        abstract class Wallet {

        }

        @BraboPrefGroup(name = "Transaction details", order = 1)
        abstract class Transaction {

        }

        @BraboPrefGroup(name = "Block details", order = 2)
        abstract class Block {

        }
    }
}

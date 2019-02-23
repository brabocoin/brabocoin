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

    @BraboPrefCategory(name = "Network", order = 1)
    abstract class NetworkCategory {

    }

    @BraboPrefCategory(name = "Categorty level 1", order = 2)
    abstract class CategoryLevel1 {

        @BraboPrefGroup(name = "Group level 3")
        abstract class GroupLevel3 {

        }
    }
}

package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotation.BraboPrefCategory;
import org.brabocoin.brabocoin.config.annotation.BraboPrefGroup;

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

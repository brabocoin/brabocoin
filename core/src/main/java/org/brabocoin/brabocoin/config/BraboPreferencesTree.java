package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotation.BraboPrefCategory;
import org.brabocoin.brabocoin.config.annotation.BraboPrefGroup;

abstract class BraboPreferencesTree {

    @BraboPrefCategory(name = "Toplevel")
    abstract class TopLevel {
        @BraboPrefGroup(name = "Group level 1")
        abstract class GroupLevel1 {

        }
    }

    @BraboPrefCategory(name = "Categorty level 1")
    abstract class CategoryLevel1 {
        @BraboPrefGroup(name = "Group level 3")
        abstract class GroupLevel3 {

        }
    }
}

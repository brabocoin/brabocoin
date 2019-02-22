package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotations.BraboPrefCategory;
import org.brabocoin.brabocoin.config.annotations.BraboPrefGroup;

public class BraboPreferencesTree {

    @BraboPrefCategory(name = "Test")
    public class Test {

        @BraboPrefGroup(name = "Test group 1")
        public class TestGroup1 {

        }
    }
}

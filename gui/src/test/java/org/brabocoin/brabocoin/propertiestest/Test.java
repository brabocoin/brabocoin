package org.brabocoin.brabocoin.propertiestest;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.stage.Stage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;


@ExtendWith(ApplicationExtension.class)
public class Test {

    public class TestConfigTest {

        public SimpleIntegerProperty bla = new SimpleIntegerProperty(10);
    }

    @Start
    private void start(Stage stage) throws IOException, InvalidPreferencesFormatException,
                                           BackingStoreException, InterruptedException {
        TestConfigTest config = new TestConfigTest();

        System.out.println(config.bla);

        SimpleBooleanProperty aanuit = new SimpleBooleanProperty(true);

        Preferences preferences = Preferences.userNodeForPackage(TestConfigTest.class);

        PreferencesFx.of(
            TestConfigTest.class,
            Category.of(
                "Category",
                Group.of(
                    "Fancy group",
                    Setting.of("aan of uit?", aanuit)
                )
            )
        ).show(true);

        preferences.exportSubtree(new FileOutputStream("dump2"));

        System.out.println(config.bla);

    }

    @org.junit.jupiter.api.Test
    void testRead() throws IOException, InvalidPreferencesFormatException {
    }
}

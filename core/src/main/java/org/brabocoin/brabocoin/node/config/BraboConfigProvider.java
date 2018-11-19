package org.brabocoin.brabocoin.node.config;

import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * Provides the default Brabocoin configuration.
 */
public class BraboConfigProvider {
    private final static String configFile = "application.yaml";

    public static ConfigurationProvider getConfig() {
        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(Paths.get(configFile));
        final ConfigurationSource source = new ClasspathConfigurationSource(configFilesProvider);

        return new ConfigurationProviderBuilder()
                .withConfigurationSource(source)
                .build();
    }
}

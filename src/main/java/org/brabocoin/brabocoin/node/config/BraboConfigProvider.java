package org.brabocoin.brabocoin.node.config;

import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;
import org.cfg4j.source.files.FilesConfigurationSource;

import java.nio.file.Paths;
import java.util.Collections;

public class BraboConfigProvider {
    private static String configFile = "application.yaml";
    private static String configDirectory = "./config";

    public static ConfigurationProvider getConfig() {
        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(Paths.get(configFile));
        final ConfigurationSource source = new FilesConfigurationSource(configFilesProvider);
        final Environment environment = new ImmutableEnvironment(configDirectory);

        return new ConfigurationProviderBuilder()
                .withConfigurationSource(source)
                .withEnvironment(environment)
                .build();
    }
}

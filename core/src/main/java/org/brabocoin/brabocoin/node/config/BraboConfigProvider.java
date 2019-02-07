package org.brabocoin.brabocoin.node.config;

import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;
import org.cfg4j.source.files.FilesConfigurationSource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Provides the default Brabocoin configuration.
 */
public class BraboConfigProvider {

    private final static String configFile = "application.yaml";

    public static ConfigurationProvider getConfig() {
        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(
            Paths.get(configFile)
        );
        final ConfigurationSource source = new ClasspathConfigurationSource(configFilesProvider);

        return new ConfigurationProviderBuilder()
            .withConfigurationSource(source)
            .build();
    }

    public static ConfigurationProvider getConfigFromFile(@NotNull String path) {
        Path p = new File(path).getAbsoluteFile().toPath();

        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(
            p.getFileName()
        );
        final ConfigurationSource source = new FilesConfigurationSource(configFilesProvider);

        return new ConfigurationProviderBuilder()
            .withConfigurationSource(source)
            .withEnvironment(new ImmutableEnvironment(p.getParent().toString()))
            .build();
    }
}

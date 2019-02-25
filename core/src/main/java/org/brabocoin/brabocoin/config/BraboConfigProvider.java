package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.model.Hash;
import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;
import org.cfg4j.source.compose.MergeConfigurationSource;
import org.cfg4j.source.context.environment.DefaultEnvironment;
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
        final ConfigurationSource source = getClassPathSource();

        ConfigurationProvider provider = new ConfigurationProviderBuilder()
            .withConfigurationSource(source)
            .build();

        // Our provider with custom type parsing
        CustomTypeConfigProvider ourProvider = new CustomTypeConfigProvider(source, new DefaultEnvironment(), provider);
        ourProvider.addParser(Hash.class, new TargetValueParser());

        return ourProvider;
    }

    public static ConfigurationProvider getConfigFromFile(@NotNull String path) {
        Path p = new File(path).getAbsoluteFile().toPath();
        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(p);

        final ConfigurationSource source = new FilesConfigurationSource(configFilesProvider);
        final ConfigurationSource fallbackSource = getClassPathSource();

        MergeConfigurationSource mergedSource = new MergeConfigurationSource(
            fallbackSource,
            source
        );
        ConfigurationProvider provider = new ConfigurationProviderBuilder()
            .withConfigurationSource(mergedSource)
            .build();


        // Our provider with custom type parsing
        CustomTypeConfigProvider ourProvider = new CustomTypeConfigProvider(mergedSource, new DefaultEnvironment(), provider);
        ourProvider.addParser(Hash.class, new TargetValueParser());

        return ourProvider;
    }

    private static ConfigurationSource getClassPathSource() {
        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(
            Paths.get(configFile)
        );
        return new ClasspathConfigurationSource(configFilesProvider);
    }
}

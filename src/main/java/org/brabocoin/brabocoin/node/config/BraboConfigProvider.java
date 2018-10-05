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
    private String configFile = "application.yaml";
    private String configDirectory = "./config";

    public BraboConfigProvider() {

    }

    public BraboConfigProvider(String configFile, String configDirectory) {
        this.configFile = configFile;
        this.configDirectory = configDirectory;
    }

    public String getConfigDirectory() {
        return configDirectory;
    }

    public String getConfigFile() {
        return configFile;
    }

    public ConfigurationProvider getConfig() {
        final ConfigFilesProvider configFilesProvider = () -> Collections.singletonList(Paths.get(getConfigFile()));
        final ConfigurationSource source = new FilesConfigurationSource(configFilesProvider);
        final Environment environment = new ImmutableEnvironment(getConfigDirectory());

        return new ConfigurationProviderBuilder()
                .withConfigurationSource(source)
                .withEnvironment(environment)
                .build();
    }
}

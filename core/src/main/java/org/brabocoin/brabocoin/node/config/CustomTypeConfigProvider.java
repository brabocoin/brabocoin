package org.brabocoin.brabocoin.node.config;

import com.github.drapostolos.typeparser.TypeParser;
import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.GenericTypeInterface;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * Configuration provider with parsing to custom property types.
 */
public class CustomTypeConfigProvider implements ConfigurationProvider {

    private final @NotNull ConfigurationProvider provider;

    public CustomTypeConfigProvider(@NotNull ConfigurationProvider provider) {
        this.provider = provider;
    }

    @Override
    public Properties allConfigurationAsProperties() {
        return provider.allConfigurationAsProperties();
    }

    @Override
    public <T> T getProperty(String key, Class<T> type) {
        TypeParser parser = TypeParser.newBuilder().
        return null;
    }

    @Override
    public <T> T getProperty(String key, GenericTypeInterface genericType) {
        return null;
    }

    @Override
    public <T> T bind(String prefix, Class<T> type) {
        return provider.bind();
    }
}

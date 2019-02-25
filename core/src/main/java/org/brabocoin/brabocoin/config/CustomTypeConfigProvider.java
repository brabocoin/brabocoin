package org.brabocoin.brabocoin.config;

import com.github.drapostolos.typeparser.NoSuchRegisteredParserException;
import com.github.drapostolos.typeparser.Parser;
import com.github.drapostolos.typeparser.TypeParser;
import com.github.drapostolos.typeparser.TypeParserBuilder;
import com.github.drapostolos.typeparser.TypeParserException;
import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.GenericTypeInterface;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * Configuration provider with parsing to custom property types.
 */
public class CustomTypeConfigProvider implements ConfigurationProvider {

    private final @NotNull TypeParserBuilder parserBuilder;
    private final @NotNull ConfigurationSource configurationSource;
    private final @NotNull Environment environment;
    private final @NotNull ConfigurationProvider provider;

    public CustomTypeConfigProvider(@NotNull ConfigurationSource configurationSource,
                                    @NotNull Environment environment,
                                    @NotNull ConfigurationProvider provider) {
        this.parserBuilder = TypeParser.newBuilder();
        this.configurationSource = configurationSource;
        this.environment = environment;
        this.provider = provider;
    }

    public <T> void addParser(Class<T> targetType, Parser<T> parser) {
        parserBuilder.registerParser(targetType, parser);
    }

    @Override
    public Properties allConfigurationAsProperties() {
        return provider.allConfigurationAsProperties();
    }

    @Override
    public <T> T getProperty(String key, Class<T> type) {
        String propertyStr = getProperty(key);
        TypeParser parser = parserBuilder.build();

        try {
            return parser.parse(propertyStr, type);
        }
        catch (TypeParserException | NoSuchRegisteredParserException e) {
            throw new IllegalArgumentException("Unable to cast value \'" + propertyStr + "\' to " + type, e);
        }
    }

    @Override
    public <T> T getProperty(String key, GenericTypeInterface genericType) {
        String propertyStr = getProperty(key);
        TypeParser parser = parserBuilder.build();

        try {
            //noinspection unchecked
            return (T)parser.parseType(propertyStr, genericType.getType());
        }
        catch (TypeParserException | NoSuchRegisteredParserException e) {
            throw new IllegalArgumentException("Unable to cast value \'" + propertyStr + "\' to " + genericType, e);
        }
    }

    @Override
    public <T> T bind(String prefix, Class<T> type) {
        // Call actual implementation using reflection
        try {
            Method method = provider.getClass().getDeclaredMethod("bind", ConfigurationProvider.class, String.class, Class.class);
            method.setAccessible(true);
            //noinspection unchecked
            return (T)method.invoke(provider, this, prefix, type);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not find binding implementation");
        }
    }

    private String getProperty(String key) {
        try {

            Object property = configurationSource.getConfiguration(environment).get(key);

            if (property == null) {
                throw new NoSuchElementException("No configuration with key: " + key);
            }

            return property.toString();

        } catch (IllegalStateException e) {
            throw new IllegalStateException("Couldn't fetch configuration from configuration source for key: " + key, e);
        }
    }
}

package org.brabocoin.brabocoin.util;

import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.util.Pair;
import org.brabocoin.brabocoin.config.MutableBraboConfig;
import org.brabocoin.brabocoin.validation.consensus.MutableConsensus;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigUtil {

    private static final String configSection = "config";
    private static final String consensusSection = "consensus";

    public static void write(MutableBraboConfig config,
                             MutableConsensus consensus,
                             File configFile) throws IOException,
                                                     IllegalAccessException {
        FileWriter writer = new FileWriter(configFile);
        Map<String, Object> configMap = new HashMap<>();
        for (Field field : MutableBraboConfig.class.getDeclaredFields()) {
            addFieldObject(configMap, field, (Property)field.get(config));
        }
        Map<String, Object> consensusMap = new HashMap<>();
        for (Field field : MutableConsensus.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object fieldObject = field.get(consensus);
            if (!Property.class.isAssignableFrom(fieldObject.getClass())) {
                continue;
            }
            addFieldObject(consensusMap, field, (Property)field.get(consensus));
        }
        Map<String, Object> yamlMap = new HashMap<>();
        yamlMap.put(configSection, configMap);
        yamlMap.put(consensusSection, consensusMap);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);

        yaml.dump(yamlMap, writer);
    }

    private static void addFieldObject(Map<String, Object> map, Field field,
                                       Property property) {
        field.setAccessible(true);
        Object result = property.getValue();
        map.put(field.getName(), result);
    }


    public static Pair<MutableBraboConfig, MutableConsensus> read(
        File configFile) throws IOException, IllegalAccessException {
        Yaml yaml = new Yaml();

        Map<String, Object> yamlMap = (Map<String, Object>)yaml.load(new FileInputStream(
            configFile));
        MutableBraboConfig config = new MutableBraboConfig();
        if (!yamlMap.containsKey(configSection)) {
            throw new IllegalArgumentException("Could not find config section");
        }
        Map<String, Object> configMap = (Map<String, Object>)yamlMap.get(configSection);

        for (Field field : MutableBraboConfig.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (!configMap.containsKey(field.getName())) {
                throw new IllegalArgumentException(String.format(
                    "Could not find value for key '%s'",
                    field.getName()
                ));
            }
            Property configProperty = (Property)field.get(config);
            Object value = configMap.get(field.getName());
            if (List.class.isAssignableFrom(value.getClass())) {
                value = FXCollections.observableArrayList(((List)value).toArray());
            }
            configProperty.setValue(value);
        }

        MutableConsensus consensus = new MutableConsensus();
        Map<String, Object> consensusMap = (Map<String, Object>)yamlMap.get(consensusSection);

        for (Map.Entry<String, Object> entry : consensusMap.entrySet()) {
            Field field;
            try {
                field = MutableConsensus.class.getDeclaredField(entry.getKey());
            }
            catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(String.format(
                    "Could not find field '%s' in consensus.",
                    entry.getKey()
                ));
            }

            field.setAccessible(true);

            ((Property)field.get(consensus)).setValue(entry.getValue());
        }

        return new Pair<>(config, consensus);
    }
}

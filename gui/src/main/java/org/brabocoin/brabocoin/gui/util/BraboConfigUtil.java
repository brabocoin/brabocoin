package org.brabocoin.brabocoin.gui.util;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javafx.beans.property.Property;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.config.BraboConfigAdapter;
import org.brabocoin.brabocoin.config.annotation.BraboPref;
import org.brabocoin.brabocoin.config.annotation.BraboPrefCategory;
import org.brabocoin.brabocoin.config.annotation.BraboPrefGroup;
import org.brabocoin.brabocoin.exceptions.IllegalConfigMappingException;
import org.brabocoin.brabocoin.gui.BrabocoinGUI;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BraboConfigUtil {

    private final static Class propertyClass = javafx.beans.property.Property.class;
    private final static Package propertyPackage = propertyClass.getPackage();
    private final static Map<Method, Property> methodPropertyMap = new HashMap<>();
    private final static Map<Method, Object> methodValueMap = new HashMap<>();

    public static PreferencesFx getConfigPreferences(
        BraboConfig config) throws IllegalConfigMappingException {
        methodPropertyMap.clear();
        methodValueMap.clear();
        PreferencesMap preferencesMap = new PreferencesMap();

        for (Method method : BraboConfig.class.getDeclaredMethods()) {
            if (method.getAnnotation(BraboPref.class) != null) {
                addPreference(method, preferencesMap, config);
            }
            else {
                try {
                    methodValueMap.put(method, method.invoke(config));
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalConfigMappingException(String.format(
                        "Could not get value of method '%s'",
                        method
                    ));
                }
            }
        }

        return instantiatePreferences(preferencesMap);
    }

    private static Comparator<Class> getComparator(
        Class<? extends Annotation> annotationClass) throws IllegalConfigMappingException {
        if (annotationClass.equals(BraboPref.class)) {
            return Comparator.comparingInt(o -> ((BraboPref)o
                .getAnnotation(annotationClass)).order());
        }
        else if (annotationClass.equals(BraboPrefGroup.class)) {
            return Comparator.comparingInt(o -> ((BraboPrefGroup)o
                .getAnnotation(annotationClass)).order());
        }
        else if (annotationClass.equals(BraboPrefCategory.class)) {
            return Comparator.comparingInt(o -> ((BraboPrefCategory)o
                .getAnnotation(annotationClass)).order());
        }

        throw new IllegalConfigMappingException(String.format(
            "Could not find comparator for annotation '%s'",
            annotationClass
        ));
    }

    private static List<Class> getTopLevelClasses(Class child) {
        Class parent = child;
        while (parent.getDeclaringClass() != null) {
            parent = parent.getDeclaringClass();
        }

        return Arrays.asList(parent.getDeclaredClasses());
    }

    private static PreferencesFx instantiatePreferences(
        PreferencesMap preferencesMap) throws IllegalConfigMappingException {
        Map<Class, Group> instantiatedGroups = buildGroupMap(preferencesMap);
        Map<Class, Category> instantiatedCategories = new HashMap<>();
        List<Setting> instantiatedSettings = preferencesMap.getSettings();

        while (preferencesMap.getCategoryMap().size() > 0) {
            Collection<Object> instantiatedObjects = new ArrayList<>(instantiatedGroups.keySet());
            instantiatedObjects.addAll(instantiatedCategories.keySet());
            instantiatedObjects.addAll(instantiatedSettings);

            for (Iterator<Map.Entry<Class, List<Object>>> it = preferencesMap.getCategoryMap()
                .entrySet()
                .iterator(); it.hasNext(); ) {
                Map.Entry<Class, List<Object>> categoryMap = it.next();
                Class categoryClass = categoryMap.getKey();
                List<Object> children = categoryMap.getValue();

                // All children have been instantiated, instantiate category
                if (instantiatedObjects.containsAll(children)) {
                    instantiatedCategories.put(
                        categoryClass,
                        deductCategory(categoryClass, children,
                            instantiatedGroups, instantiatedCategories,
                            preferencesMap
                        )
                    );
                    it.remove();
                }
            }
        }

        Class anyGroup = preferencesMap.getGroupMap().keySet().iterator().next();

        return PreferencesFx.of(
            BrabocoinGUI.class,
            getTopLevelClasses(anyGroup).stream()
                .sorted(getComparator(BraboPrefCategory.class))
                .map(instantiatedCategories::get)
                .toArray(Category[]::new)
        );
    }

    private static Category deductCategory(Class categoryClass, List<Object> children,
                                           Map<Class, Group> instantiatedGroups,
                                           Map<Class, Category> instantiatedCategories,
                                           PreferencesMap preferencesMap) throws IllegalConfigMappingException {
        Category category = null;

        BraboPrefCategory braboPrefCategory = (BraboPrefCategory)categoryClass.getAnnotation(
            BraboPrefCategory.class);

        if (children.stream().allMatch(o -> o instanceof Class)) {
            List<Class> childerenClasses = children.stream()
                .map(o -> (Class)o)
                .collect(Collectors.toList());
            if (childerenClasses.stream().anyMatch(instantiatedGroups::containsKey)) {
                // Create category with groups
                category = Category.of(braboPrefCategory.name(), childerenClasses.stream()
                    .filter(instantiatedGroups::containsKey)
                    .sorted(getComparator(BraboPrefGroup.class))
                    .map(instantiatedGroups::get).toArray(Group[]::new));
            }
            if (childerenClasses.stream().anyMatch(instantiatedCategories::containsKey)) {
                Category[] subcategories = childerenClasses.stream()
                    .filter(instantiatedCategories::containsKey)
                    .sorted(getComparator(BraboPrefCategory.class))
                    .map(instantiatedCategories::get).toArray(Category[]::new);

                if (category == null) {
                    category = Category.of(braboPrefCategory.name());
                }

                category = category.subCategories(subcategories);
            }
        }
        else if (children.stream().allMatch(o -> o instanceof Setting)) {
            Setting[] settings = children.stream()
                .map(o -> (Setting)o)
                .map(s -> preferencesMap.getPrefMap().get(s))
                .sorted(Comparator.comparingInt(BraboPref::order))
                .map(p -> preferencesMap.getPrefMap().inverse().get(p))
                .toArray(Setting[]::new);
            category = Category.of(
                braboPrefCategory.name(), settings
            );
        }
        else {
            throw new IllegalConfigMappingException("Children of class '%s' contains mixed types.");
        }

        return category;
    }

    private static Map<Class, Group> buildGroupMap(
        PreferencesMap preferencesMap) throws IllegalConfigMappingException {
        Map<Class, List<Setting>> groupSettingMap = preferencesMap.getGroupMap();

        Map<Class, Group> groupMap = new HashMap<>();
        for (Class groupClass : groupSettingMap.keySet()) {
            Annotation prefGroupAnnotation = groupClass.getAnnotation(BraboPrefGroup.class);
            if (prefGroupAnnotation == null) {
                throw new IllegalConfigMappingException(String.format(
                    "Group class '%s' is not annotated as a preference group.",
                    groupClass
                ));
            }
            BraboPrefGroup prefGroup = (BraboPrefGroup)prefGroupAnnotation;

            Group group;
            if (prefGroup.name().equals("")) {
                group = Group.of(groupSettingMap.get(groupClass).toArray(new Setting[0]));
            }
            else {
                group = Group.of(
                    prefGroup.name(),
                    groupSettingMap.get(groupClass).toArray(new Setting[0])
                );
            }

            groupMap.put(groupClass, group);
        }

        return groupMap;
    }

    private static void addPreference(Method method,
                                      PreferencesMap preferencesMap,
                                      BraboConfig config) throws IllegalConfigMappingException {
        BraboPref pref = method.getAnnotation(BraboPref.class);
        Setting setting = deductSetting(method, pref, config);
        preferencesMap.getPrefMap().put(setting, pref);

        if (pref.destination().getAnnotation(BraboPrefGroup.class) != null) {
            if (!preferencesMap.getGroupMap().containsKey(pref.destination())) {
                preferencesMap.getGroupMap().put(pref.destination(), new ArrayList<>());

                fillCategoryMap(pref.destination(), preferencesMap);
            }

            preferencesMap.getGroupMap().get(pref.destination()).add(
                setting
            );

            preferencesMap.getGroupMap().get(pref.destination()).sort(
                Comparator.comparingInt(s -> preferencesMap.getPrefMap().get(s).order())
            );
        }
        else if (pref.destination().getAnnotation(BraboPrefCategory.class) != null) {
            if (!preferencesMap.getCategoryMap().containsKey(pref.destination())) {
                preferencesMap.getCategoryMap().put(pref.destination(), new ArrayList<>());

                fillCategoryMap(pref.destination(), preferencesMap);
            }

            preferencesMap.getCategoryMap().get(pref.destination()).add(
                setting
            );
        }

    }

    private static void fillCategoryMap(Class child,
                                        PreferencesMap preferencesMap) {
        Class parentClass = child.getDeclaringClass();

        if (parentClass == null || parentClass.getAnnotation(BraboPrefCategory.class) == null) {
            return;
        }

        if (!preferencesMap.getCategoryMap().containsKey(parentClass)) {
            List<Object> childList = new ArrayList<>();
            childList.add(child);
            preferencesMap.getCategoryMap().put(parentClass, childList);
            fillCategoryMap(parentClass, preferencesMap);
            return;
        }

        if (preferencesMap.getCategoryMap().get(parentClass).contains(child)) {
            return;
        }

        preferencesMap.getCategoryMap().get(parentClass).add(child);
    }

    private static Setting deductSetting(Method method,
                                         BraboPref pref,
                                         BraboConfig config) throws IllegalConfigMappingException {
        Class returnType = method.getReturnType();

        // Determine JavaFX property classes.
        Class<?> returnProperty = findPropetyClass(returnType, false);
        Class<?> returnSimpleProperty = findPropetyClass(returnType, true);

        Constructor simplePropertyConstructor;
        try {
            simplePropertyConstructor = returnSimpleProperty.getConstructor(returnType);
        }
        catch (NoSuchMethodException e) {
            // Attempt to get primitive type class
            try {
                Field typeField = returnType.getField("TYPE");
                Class primitive = (Class)typeField.get(returnType);

                simplePropertyConstructor = returnSimpleProperty.getConstructor(primitive);
            }
            catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e1) {
                throw new IllegalConfigMappingException(String.format(
                    "Could not find constructor for '%s' on parameter type '%s'.",
                    returnSimpleProperty,
                    returnType
                ));
            }
        }

        Object configValue;
        try {
            configValue = method.invoke(config);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalConfigMappingException(String.format(
                "Could not invoke method '%s' on config object.",
                method
            ));
        }

        Object simpleProperty;
        try {
            simpleProperty = simplePropertyConstructor.newInstance(configValue);
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalConfigMappingException(String.format(
                "Could not invoke constructor '%s' with parameter '%s'.",
                simplePropertyConstructor, configValue
            ));
        }

        methodPropertyMap.put(method, (Property)simpleProperty);

        Method settingInstantiationMethod;
        try {
            settingInstantiationMethod = Setting.class.getMethod(
                "of",
                String.class,
                returnProperty
            );
        }
        catch (NoSuchMethodException e) {
            throw new IllegalConfigMappingException(String.format(
                "Could not find PreferencesFX setting instantiation method for property class '%s'",
                returnProperty
            ));
        }

        Setting setting;
        try {
            setting = (Setting)settingInstantiationMethod.invoke(null, pref.name(), simpleProperty);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalConfigMappingException(String.format(
                "Could not invoke PreferencesFX setting instantiation method for property class "
                    + "'%s'",
                simpleProperty
            ));
        }

        return setting;
    }

    private static Class findPropetyClass(Class type,
                                          boolean simple) throws IllegalConfigMappingException {
        String propertyClassName = (simple ? "Simple" : "") + type.getSimpleName() + "Property";

        try {
            return Class.forName(deductQualifiedPropertyClassName(propertyClassName));
        }
        catch (ClassNotFoundException e) {
            throw new IllegalConfigMappingException(String.format(
                "Could not find property class for type '%s'",
                type
            ));
        }
    }

    private static String deductQualifiedPropertyClassName(String propertyClassName) {
        return String.format(
            "%s.%s",
            propertyPackage.getName(),
            propertyClassName
        );
    }

    public static void updateConfig(BraboConfigAdapter braboConfigAdapter) {
        braboConfigAdapter.setDelegator(buildConfig());
    }

    private static BraboConfig buildConfig() {
        return (BraboConfig)Proxy.newProxyInstance(BraboConfig.class.getClassLoader(),
            new Class[] {BraboConfig.class}, new BraboConfigPropertyProxy()
        );
    }

    public static class BraboConfigPropertyProxy implements InvocationHandler {

        private final Map<Method, Property> methodPropertyMap;
        private final Map<Method, Object> methodValueMap;

        public BraboConfigPropertyProxy() {
            this.methodPropertyMap = new HashMap<>(BraboConfigUtil.methodPropertyMap);
            this.methodValueMap = new HashMap<>(BraboConfigUtil.methodValueMap);
        }

        public Object invoke(Object proxy, Method m,
                             Object[] args) throws IllegalConfigMappingException {
            if (methodPropertyMap.containsKey(m)) {
                return methodPropertyMap.get(m).getValue();
            }
            else if (methodValueMap.containsKey(m)) {
                return methodValueMap.get(m);
            }
            else {
                throw new IllegalConfigMappingException(
                    String.format(
                        "Could not find mapping of method '%s' for proxy",
                        m
                    )
                );
            }
        }
    }

    private static class PreferencesMap {

        private Map<Class, List<Object>> categoryMap = new HashMap<>();
        private Map<Class, List<Setting>> groupMap = new HashMap<>();
        private BiMap<Setting, BraboPref> prefMap = HashBiMap.create();

        Map<Class, List<Setting>> getGroupMap() {
            return groupMap;
        }

        Map<Class, List<Object>> getCategoryMap() {
            return categoryMap;
        }

        public BiMap<Setting, BraboPref> getPrefMap() {
            return prefMap;
        }

        List<Setting> getSettings() {
            return Stream.concat(
                categoryMap.values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(o -> o instanceof Setting)
                    .map(o -> (Setting)o),
                groupMap.values().stream().flatMap(List::stream)
            ).collect(Collectors.toList());
        }
    }

    public static void writeConfig(BraboConfig config,
                                   String configPath) throws IllegalConfigMappingException,
                                                             IOException {
        Yaml yaml = new Yaml();

        FileWriter writer = new FileWriter(configPath, false);
        writer.write(yaml.dump(getConfigMap(config)));
        writer.close();
    }

    private static Map<String, Object> getConfigMap(
        BraboConfig config) throws IllegalConfigMappingException {
        Map<String, Object> configMap = new HashMap<>();
        for (Method method : BraboConfig.class.getDeclaredMethods()) {

            Object object;
            try {
                object = method.invoke(config);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalConfigMappingException(String.format(
                    "Could not invoke method '%s' on config object",
                    method
                ));
            }

            if (object instanceof Hash) {
                object = ByteUtil.toHexString(((Hash)object).getValue(), Constants.BLOCK_HASH_SIZE);
            }
            configMap.put(method.getName(), object);
        }

        return new HashMap<String, Object>() {{
            put(BraboConfig.braboConfigSection, configMap);
        }};
    }
}

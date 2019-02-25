package org.brabocoin.brabocoin.gui.util;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BraboConfigPreferencesFX {

    private final Class propertyClass = javafx.beans.property.Property.class;
    private final Package propertyPackage = propertyClass.getPackage();
    private final Map<Method, Property> methodPropertyMap;
    private final Map<Method, Object> methodValueMap;
    private final Map<Method, Object> parentConfigValueMap;

    public BraboConfigPreferencesFX() {
        methodPropertyMap = new HashMap<>();
        methodValueMap = new HashMap<>();
        parentConfigValueMap = new HashMap<>();
    }

    /**
     * Creates a {@link PreferencesFx} model for the given {@link BraboConfig}, using the
     * Preference Tree references by the {@link BraboPref} annotations for each option in the
     * config.
     * <p>
     * Also fills the {@link #methodPropertyMap} and {@link #methodValueMap} to allow config
     * update using {@link #updateConfig)}
     *
     * @param config
     *     The config to build a PreferencesFX model for.
     * @return A {@link PreferencesFx} model
     * @throws IllegalConfigMappingException
     *     Thrown when any mapping fails.
     */
    public PreferencesFx getConfigPreferences(
        BraboConfig config) throws IllegalConfigMappingException {
        // Create a new data holder for structure building
        PreferencesStructure preferencesStructure = new PreferencesStructure();

        // Clear the current method maps
        methodPropertyMap.clear();
        methodValueMap.clear();
        parentConfigValueMap.clear();

        // Loop over declared methods in config
        for (Method method : BraboConfig.class.getDeclaredMethods()) {
            if (method.getAnnotation(BraboPref.class) != null) {
                // Add preference for this config
                addPreference(method, preferencesStructure, config);
            }
            else {
                // Else, just store the value of the config.
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

        return instantiatePreferencesFx(preferencesStructure);
    }

    /**
     * Get class comparator, using the given annotation.
     *
     * @param annotationClass
     *     Annotation class to use
     * @return Comparator for class
     * @throws IllegalConfigMappingException
     *     Thrown when the given class is not a BraboPref annotation.
     */
    private Comparator<Class> getComparator(
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

    /**
     * Get top level class given a class descendant.
     *
     * @param child
     *     The child to find the top level class for
     * @return Top level class
     */
    private List<Class> getTopLevelClasses(Class child) {
        Class parent = child;
        while (parent.getDeclaringClass() != null) {
            parent = parent.getDeclaringClass();
        }

        return Arrays.asList(parent.getDeclaredClasses());
    }

    /**
     * Instantiates a {@link PreferencesFx} object given the preference structure.
     *
     * @param preferencesStructure
     *     Preference structure to use.
     * @return PreferenceFx object.
     * @throws IllegalConfigMappingException
     *     Thrown when mapping fails.
     */
    private PreferencesFx instantiatePreferencesFx(
        PreferencesStructure preferencesStructure) throws IllegalConfigMappingException {
        // Build the groups using the preference structure
        Map<Class, Group> instantiatedGroups = buildGroupMap(preferencesStructure);
        // Map to store instantiated categories
        Map<Class, Category> instantiatedCategories = new HashMap<>();
        // Get all instantiated settings
        List<Setting> instantiatedSettings = preferencesStructure.getSettings();

        // While we still have categories to instantiate
        while (preferencesStructure.getCategoryMap().size() > 0) {
            // Combine all instantiated objects
            Collection<Object> instantiatedObjects = new ArrayList<>(instantiatedGroups.keySet());
            instantiatedObjects.addAll(instantiatedCategories.keySet());
            instantiatedObjects.addAll(instantiatedSettings);

            // Loop over category map
            for (Iterator<Map.Entry<Class, List<Object>>> it = preferencesStructure.getCategoryMap()
                .entrySet()
                .iterator(); it.hasNext(); ) {
                Map.Entry<Class, List<Object>> categoryMap = it.next();
                // Category class
                Class categoryClass = categoryMap.getKey();
                // Children in the category.
                // Note: this can be a mix of classes that are annotated to be groups or
                // categories, or solely instantiated setting objects.
                List<Object> children = categoryMap.getValue();

                // All children have been instantiated, instantiate this category
                if (instantiatedObjects.containsAll(children)) {
                    instantiatedCategories.put(
                        categoryClass,
                        deductCategory(categoryClass, children,
                            instantiatedGroups, instantiatedCategories,
                            preferencesStructure
                        )
                    );
                    it.remove();
                }
            }
        }

        Class anyGroup = preferencesStructure.getGroupMap().keySet().iterator().next();

        return PreferencesFx.of(
            BrabocoinGUI.class,
            getTopLevelClasses(anyGroup).stream()
                .sorted(getComparator(BraboPrefCategory.class))
                .map(instantiatedCategories::get)
                .toArray(Category[]::new)
        );
    }

    /**
     * Deduct a {@link Category} object.
     *
     * @param categoryClass
     *     The category class in the preference tree.
     * @param children
     *     The children of this category, all of which have been instantiated.
     * @param instantiatedGroups
     *     Mapping of classes that are annotated with {@link BraboPrefGroup} to {@link Group}
     *     objects
     * @param instantiatedCategories
     *     Mapping of classes that are annotated with {@link BraboPrefCategory} to
     *     {@link Category} objects
     * @param preferencesStructure
     *     The current preference structure.
     * @return {@link Category} object deducted.
     * @throws IllegalConfigMappingException
     *     Thrown when mapping fails or children are of mixed values.
     */
    private Category deductCategory(Class categoryClass, List<Object> children,
                                    Map<Class, Group> instantiatedGroups,
                                    Map<Class, Category> instantiatedCategories,
                                    PreferencesStructure preferencesStructure) throws IllegalConfigMappingException {
        Category category = null;

        BraboPrefCategory braboPrefCategory = (BraboPrefCategory)categoryClass.getAnnotation(
            BraboPrefCategory.class);


        // If any of the children are settings
        if (children.stream().anyMatch(o -> o instanceof Setting)) {
            // Get settings, and order them by their annotated order
            Setting[] settings = children.stream()
                .filter(o -> o instanceof Setting)
                .map(o -> (Setting)o)
                .map(s -> preferencesStructure.getPrefMap().get(s))
                .sorted(Comparator.comparingInt(BraboPref::order))
                .map(p -> preferencesStructure.getPrefMap().inverse().get(p))
                .toArray(Setting[]::new);
            category = Category.of(
                braboPrefCategory.name(), settings
            );
        }

        // If any of the children are class objects
        if (children.stream().anyMatch(o -> o instanceof Class)) {
            // Get class objects
            List<Class> classChildren = children.stream()
                .filter(o -> o instanceof Class)
                .map(o -> (Class)o)
                .collect(Collectors.toList());
            // If any child is a group class
            if (classChildren.stream().anyMatch(instantiatedGroups::containsKey)) {
                if (category != null) {
                    throw new IllegalConfigMappingException(String.format(
                        "Category '%s' cannot have both settings and groups.", categoryClass));
                }
                // Create category with groups
                category = Category.of(braboPrefCategory.name(), classChildren.stream()
                    .filter(instantiatedGroups::containsKey)
                    .sorted(getComparator(BraboPrefGroup.class))
                    .map(instantiatedGroups::get).toArray(Group[]::new));
            }
            // If any child is a category class
            if (classChildren.stream().anyMatch(instantiatedCategories::containsKey)) {
                // Get the instantiated child categories
                Category[] subcategories = classChildren.stream()
                    .filter(instantiatedCategories::containsKey)
                    .sorted(getComparator(BraboPrefCategory.class))
                    .map(instantiatedCategories::get).toArray(Category[]::new);

                // If the category does not have any groups, instantiate an empty category.
                if (category == null) {
                    category = Category.of(braboPrefCategory.name());
                }

                // Add the child subcategories
                category = category.subCategories(subcategories);
            }
        }

        if (category == null) {
            throw new IllegalConfigMappingException(String.format(
                "Children of class '%s' contains mixed types or no children.",
                categoryClass
            ));
        }

        return category;
    }

    /**
     * Builds a group map for a given preference structure.
     *
     * @param preferencesStructure
     *     Preference structure.
     * @return Map of {@link BraboPrefGroup} classes to their instantiated {@link Group} objects.
     * @throws IllegalConfigMappingException
     *     Thrown when a class does not have the {@link BraboPrefGroup} annotation
     */
    private Map<Class, Group> buildGroupMap(
        PreferencesStructure preferencesStructure) throws IllegalConfigMappingException {
        Map<Class, List<Setting>> groupSettingMap = preferencesStructure.getGroupMap();

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

    /**
     * Attempts to deduct a Setting object from this method and manipulate
     * {@param preferencesStructure} to match the structure defined in the annotation.
     *
     * @param method
     *     The method to build a preference for.
     * @param preferencesStructure
     *     The current preference structure.
     * @param config
     *     The config the preference is build for.
     * @throws IllegalConfigMappingException
     *     Thrown when mapping fails.
     */
    private void addPreference(Method method,
                               PreferencesStructure preferencesStructure,
                               BraboConfig config) throws IllegalConfigMappingException {
        BraboPref pref = method.getAnnotation(BraboPref.class);

        // Create setting for method
        Setting setting = deductSetting(method, pref, config);
        // Store in structure
        preferencesStructure.getPrefMap().put(setting, pref);

        // If the preferences is in a group
        if (pref.destination().getAnnotation(BraboPrefGroup.class) != null) {
            if (!preferencesStructure.getGroupMap().containsKey(pref.destination())) {
                preferencesStructure.getGroupMap().put(pref.destination(), new ArrayList<>());

                // Recurse structure
                fillCategoryMap(pref.destination(), preferencesStructure);
            }

            // Add setting to group map
            preferencesStructure.getGroupMap().get(pref.destination()).add(
                setting
            );

            // Sort group map according to order
            preferencesStructure.getGroupMap().get(pref.destination()).sort(
                Comparator.comparingInt(s -> preferencesStructure.getPrefMap().get(s).order())
            );
        }
        else if (pref.destination().getAnnotation(BraboPrefCategory.class) != null) {
            if (!preferencesStructure.getCategoryMap().containsKey(pref.destination())) {
                preferencesStructure.getCategoryMap().put(pref.destination(), new ArrayList<>());

                // Recurse structure
                fillCategoryMap(pref.destination(), preferencesStructure);
            }

            // Add setting to group map
            preferencesStructure.getCategoryMap().get(pref.destination()).add(
                setting
            );
        }

    }

    /**
     * Recursively determine categories given a child.
     * Defines categories if needed, until an existing category is found or the root is reached.
     *
     * @param child
     *     The child to determine the category map for
     * @param preferencesStructure
     *     The current preferences structure
     */
    private void fillCategoryMap(Class child,
                                 PreferencesStructure preferencesStructure) {
        Class parentClass = child.getDeclaringClass();

        if (parentClass == null || parentClass.getAnnotation(BraboPrefCategory.class) == null) {
            return;
        }

        if (!preferencesStructure.getCategoryMap().containsKey(parentClass)) {
            List<Object> childList = new ArrayList<>();
            childList.add(child);
            preferencesStructure.getCategoryMap().put(parentClass, childList);
            fillCategoryMap(parentClass, preferencesStructure);
            return;
        }

        if (preferencesStructure.getCategoryMap().get(parentClass).contains(child)) {
            return;
        }

        preferencesStructure.getCategoryMap().get(parentClass).add(child);
    }

    /**
     * Uses reflection to create a {@link Setting} object for a given method.
     *
     * @param method
     *     The method to create a setting for
     * @param pref
     *     The {@link BraboPref} annotation for this method
     * @param config
     *     The config file to determine the initial value of the setting
     * @return A setting for the given method.
     * @throws IllegalConfigMappingException
     *     Thrown when some mapping fails during construction.
     */
    private Setting deductSetting(Method method,
                                  BraboPref pref,
                                  BraboConfig config) throws IllegalConfigMappingException {
        Class returnType = method.getReturnType();
        if (returnType.equals(Hash.class)) {
            returnType = String.class;
        }

        // Determine JavaFX property classes.
        Class<?> returnProperty = findPropertyClass(returnType, false);
        Class<?> returnSimpleProperty = findPropertyClass(returnType, true);

        // Get constructor for the simple property
        Constructor simplePropertyConstructor;
        try {
            simplePropertyConstructor = returnSimpleProperty.getConstructor(returnType);
        }
        catch (NoSuchMethodException e) {
            // Attempt to get primitive type class
            try {
                Field typeField = returnType.getField("TYPE");
                Class primitive = (Class)typeField.get(returnType);

                // Get constructor using primitive type
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

        // Get the value using current config.
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

        // Convert hash to string
        if (configValue.getClass().equals(Hash.class)) {
            configValue = ByteUtil.toHexString(
                ((Hash)configValue).getValue(),
                Constants.BLOCK_HASH_SIZE
            );
        }

        // Store previous config value
        parentConfigValueMap.put(method, configValue);

        // Instantiate simple property
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

        // Store the method property mapping
        methodPropertyMap.put(method, (Property)simpleProperty);

        // Get Setting instantiation method for property
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

        // Invoke Setting instantiation method for property
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

    /**
     * Finds the {@link Property} class for a given type.
     *
     * @param type
     *     The type to find a property for
     * @param simple
     *     Whether or not to find the 'Simple' property class
     * @return Property class
     * @throws IllegalConfigMappingException
     *     Thrown when the property class is not found
     */
    private Class findPropertyClass(Class type,
                                    boolean simple) throws IllegalConfigMappingException {
        if (type.equals(Hash.class)) {
            type = String.class;
        }

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

    /**
     * Returns the fully qualified property class name.
     *
     * @param propertyClassName
     *     Simple property class name.
     * @return Qualified property class name.
     */
    private String deductQualifiedPropertyClassName(String propertyClassName) {
        return String.format(
            "%s.%s",
            propertyPackage.getName(),
            propertyClassName
        );
    }

    /**
     * Updates the config by building a new config and replacing the delegator of the config
     * adapter.
     *
     * @param braboConfigAdapter
     *     The config adapter to update.
     * @return Whether the config has changed
     */
    public boolean updateConfig(
        BraboConfigAdapter braboConfigAdapter) throws IllegalConfigMappingException {
        if (methodPropertyMap.size() + methodValueMap.size() <= 0) {
            throw new IllegalStateException("Mappings are empty, model should be build first.");
        }
        BraboConfig config = buildConfig();

        boolean changes;
        try {
            changes = !isEqualConfig(braboConfigAdapter, config);
        }
        catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalConfigMappingException("Could not get value in config", e);
        }

        braboConfigAdapter.setDelegator(config);

        return changes;
    }

    /**
     * Returns whether the given configs are equal.
     *
     * @param config1
     *     The first config
     * @param config2
     *     The second config
     * @return True when the configs are equal
     * @throws InvocationTargetException
     *     Thrown when the method in a config cannot be invoked.
     * @throws IllegalAccessException
     *     Thrown when a method in a config could not be accessed.
     */
    private boolean isEqualConfig(BraboConfig config1,
                                  BraboConfig config2) throws InvocationTargetException,
                                                              IllegalAccessException {
        for (Method m : BraboConfig.class.getDeclaredMethods()) {
            if (!m.invoke(config1).equals(m.invoke(config2))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Build a config file using the {@link #methodValueMap} and {@link #methodPropertyMap}.
     *
     * @return The generated config.
     */
    private BraboConfig buildConfig() {
        return (BraboConfig)Proxy.newProxyInstance(BraboConfig.class.getClassLoader(),
            new Class[] {BraboConfig.class}, new BraboConfigPropertyProxy()
        );
    }

    /**
     * Write the config in yaml format to the config path.
     *
     * @param config
     *     The config to write
     * @param configPath
     *     The config path to write the config to
     * @throws IllegalConfigMappingException
     *     Thrown when mapping errors occur.
     * @throws IOException
     *     Thrown when the file could not be written.
     */
    public void writeConfig(BraboConfig config,
                            String configPath) throws IllegalConfigMappingException,
                                                      IOException {
        Yaml yaml = new Yaml();

        FileWriter writer = new FileWriter(configPath, false);
        writer.write(yaml.dump(getConfigMap(config)));
        writer.close();
    }

    /**
     * Create a map that can be dumped to yaml format.
     *
     * @param config
     *     The config to create a map for
     * @return Map of objects
     * @throws IllegalConfigMappingException
     *     Thrown when mapping errors occur
     */
    private Map<String, Object> getConfigMap(
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

    /**
     * Proxy to dynamically instantiate the {@link BraboConfig} interface.
     */
    private class BraboConfigPropertyProxy implements InvocationHandler {

        private Object mapObject(Method m, Object object) {
            if (m.getReturnType().equals(Hash.class) && object.getClass().equals(String.class)) {
                try {
                    return new Hash(ByteUtil.fromHexString((String)object));
                }
                catch (IllegalArgumentException e) {
                    GUIUtils.displayErrorDialog(
                        "Invalid config mapping.",
                        String.format("Could not convert value for config '%s'", m.getName()),
                        String.format(
                            "Value '%s' is not a valid '%s', resetting to previous value.",
                            object,
                            m.getReturnType().getSimpleName()
                        )
                    );
                    // Get old value
                    String previousHash = (String)parentConfigValueMap.get(m);
                    if (methodPropertyMap.containsKey(m)) {
                        methodPropertyMap.put(m, new SimpleStringProperty(
                            previousHash
                        ));
                    }
                    else if (methodValueMap.containsKey(m)) {
                        methodValueMap.put(m, new SimpleStringProperty(
                            previousHash
                        ));
                    }
                    return new Hash(ByteUtil.fromHexString(previousHash));
                }
            }

            return object;
        }

        public Object invoke(Object proxy, Method m,
                             Object[] args) throws IllegalConfigMappingException {
            if (methodPropertyMap.containsKey(m)) {
                return mapObject(m, methodPropertyMap.get(m).getValue());
            }
            else if (methodValueMap.containsKey(m)) {
                return mapObject(m, methodValueMap.get(m));
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

    /**
     * Represents the current Preferences structure.
     */
    private class PreferencesStructure {

        private Map<Class, List<Object>> categoryMap = new HashMap<>();
        private Map<Class, List<Setting>> groupMap = new HashMap<>();
        private BiMap<Setting, BraboPref> prefMap = HashBiMap.create();

        Map<Class, List<Setting>> getGroupMap() {
            return groupMap;
        }

        Map<Class, List<Object>> getCategoryMap() {
            return categoryMap;
        }

        BiMap<Setting, BraboPref> getPrefMap() {
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
}

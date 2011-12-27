package org.systemsbiology.addama.jsonconfig.impls;

import org.systemsbiology.addama.jsonconfig.Mapping;

import java.util.Map;

/**
 * @author hrovira
 */
public abstract class MappingPropertyByIdContainer<T> {
    protected final Map<String, T> propertiesById;
    protected final String propertyName;
    protected final T defaultValue;

    public MappingPropertyByIdContainer(Map<String, T> map, String propertyName) {
        this(map, propertyName, null);
    }

    public MappingPropertyByIdContainer(Map<String, T> map, String propertyName, T defaultValue) {
        this.propertiesById = map;
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    protected boolean jsonHasProperty(Mapping mapping) {
        if (mapping.JSON().has(this.propertyName)) {
            return true;
        }

        this.addDefaultValue(mapping);
        return false;
    }

    protected void addProperty(Mapping mapping, T value) {
        if (value != null) {
            this.propertiesById.put(mapping.id, value);
        } else {
            this.addDefaultValue(mapping);
        }
    }

    protected void addDefaultValue(Mapping mapping) {
        if (this.defaultValue != null) {
            this.propertiesById.put(mapping.id, this.defaultValue);
        }
    }

}

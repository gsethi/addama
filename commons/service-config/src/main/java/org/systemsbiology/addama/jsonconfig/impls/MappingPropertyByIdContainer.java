package org.systemsbiology.addama.jsonconfig.impls;

import org.systemsbiology.addama.jsonconfig.Mapping;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public abstract class MappingPropertyByIdContainer<T> {
    private final Map<String, T> valuesById;
    private final T defaultValue;

    protected final String propertyName;

    public MappingPropertyByIdContainer(Map<String, T> map) {
        this.valuesById = map;
        this.propertyName = null;
        this.defaultValue = null;
    }

    public MappingPropertyByIdContainer(Map<String, T> map, String propertyName) {
        this.valuesById = map;
        this.propertyName = propertyName;
        this.defaultValue = null;
    }

    public MappingPropertyByIdContainer(Map<String, T> map, String propertyName, T defaultValue) {
        this.valuesById = map;
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    protected boolean jsonHasProperty(Mapping mapping) {
        if (!isEmpty(this.propertyName)) {
            if (mapping.JSON().has(this.propertyName)) {
                return true;
            }
        }

        this.addDefaultValue(mapping);
        return false;
    }

    protected void addValue(Mapping mapping, T value) {
        if (value != null) {
            this.valuesById.put(mapping.ID(), value);
        } else {
            this.addDefaultValue(mapping);
        }
    }

    protected void addDefaultValue(Mapping mapping) {
        if (this.defaultValue != null) {
            this.valuesById.put(mapping.ID(), this.defaultValue);
        }
    }

}

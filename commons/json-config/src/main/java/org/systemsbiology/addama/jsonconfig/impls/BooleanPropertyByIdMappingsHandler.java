package org.systemsbiology.addama.jsonconfig.impls;

import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;

import java.util.Map;

/**
 * @author hrovira
 */
public class BooleanPropertyByIdMappingsHandler extends MappingPropertyByIdContainer<Boolean> implements MappingsHandler {
    public BooleanPropertyByIdMappingsHandler(Map<String, Boolean> map, String propertyName) {
        super(map, propertyName);
    }

    public BooleanPropertyByIdMappingsHandler(Map<String, Boolean> map, String propertyName, Boolean defaultValue) {
        super(map, propertyName, defaultValue);
    }

    public void handle(Mapping mapping) throws Exception {
        if (jsonHasProperty(mapping)) {
            addProperty(mapping, mapping.JSON().getBoolean(this.propertyName));
        }
    }
}

package org.systemsbiology.addama.jsonconfig.impls;

import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;

import java.util.Map;

/**
 * @author hrovira
 */
public class IntegerPropertyByIdMappingsHandler extends MappingPropertyByIdContainer<Integer> implements MappingsHandler {

    public IntegerPropertyByIdMappingsHandler(Map<String, Integer> map, String propertyName) {
        super(map, propertyName);
    }

    public IntegerPropertyByIdMappingsHandler(Map<String, Integer> stringIntegerMap, String propertyName, Integer defaultValue) {
        super(stringIntegerMap, propertyName, defaultValue);
    }

    public void handle(Mapping mapping) throws Exception {
        if (jsonHasProperty(mapping)) {
            this.addProperty(mapping, mapping.JSON().getInt(this.propertyName));
        }
    }
}

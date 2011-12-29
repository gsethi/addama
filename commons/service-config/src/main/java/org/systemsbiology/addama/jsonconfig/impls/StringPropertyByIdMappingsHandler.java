package org.systemsbiology.addama.jsonconfig.impls;

import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;

import java.util.Map;

/**
 * @author hrovira
 */
public class StringPropertyByIdMappingsHandler extends MappingPropertyByIdContainer<String> implements MappingsHandler {

    public StringPropertyByIdMappingsHandler(Map<String, String> map, String propertyName) {
        super(map, propertyName);
    }

    public StringPropertyByIdMappingsHandler(Map<String, String> map, String propertyName, String defaultValue) {
        super(map, propertyName, defaultValue);
    }

    public void handle(Mapping mapping) throws Exception {
        if (this.jsonHasProperty(mapping)) {
            this.addValue(mapping, mapping.JSON().getString(this.propertyName));
        }
    }
}

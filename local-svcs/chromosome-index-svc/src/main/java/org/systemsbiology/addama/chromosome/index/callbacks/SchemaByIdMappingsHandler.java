package org.systemsbiology.addama.chromosome.index.callbacks;

import org.systemsbiology.addama.chromosome.index.pojos.Schema;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;

import java.util.Map;

/**
 * @author hrovira
 */
public class SchemaByIdMappingsHandler extends MappingPropertyByIdContainer<Schema> implements MappingsHandler {

    public SchemaByIdMappingsHandler(Map<String, Schema> stringSchemaMap) {
        super(stringSchemaMap, "schema");
    }

    public void handle(Mapping mapping) throws Exception {
        if (this.jsonHasProperty(mapping)) {
            this.addValue(mapping, new Schema(mapping.JSON().getJSONObject("schema")));
        }
    }
}

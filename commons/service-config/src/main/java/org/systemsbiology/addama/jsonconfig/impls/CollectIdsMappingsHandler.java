package org.systemsbiology.addama.jsonconfig.impls;

import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;

import java.util.Collection;

/**
 * @author hrovira
 */
public class CollectIdsMappingsHandler implements MappingsHandler {
    private final Collection<String> ids;

    public CollectIdsMappingsHandler(Collection<String> ids) {
        this.ids = ids;
    }

    public void handle(Mapping mapping) throws Exception {
        this.ids.add(mapping.ID());
    }
}

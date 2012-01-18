package org.systemsbiology.addama.services.execution.args;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategy.Strategy;

import java.util.Map;

import static org.systemsbiology.addama.services.execution.args.ArgumentStrategy.Strategy.valueOf;

/**
 * @author hrovira
 */
public class ArgumentStrategyMappingsHandler extends MappingPropertyByIdContainer<ArgumentStrategy> implements MappingsHandler {

    public ArgumentStrategyMappingsHandler(Map<String, ArgumentStrategy> map) {
        super(map);
    }

    public void handle(Mapping mapping) throws Exception {
        JSONObject item = mapping.JSON();
        if (item.has("argumentStrategy")) {
            Strategy strategy = valueOf(item.getString("argumentStrategy"));
            switch (strategy) {
                case formDataAsFile:
                    addValue(mapping, new FormDataAsFileArgumentStrategy());
                    break;
                case parseScriptVars:
                    addValue(mapping, new ParseScriptVarsArgumentStrategy());
                    break;
                default:
                    addValue(mapping, new DefaultArgumentStrategy());
            }
        } else {
            addValue(mapping, new DefaultArgumentStrategy());
        }

    }
}

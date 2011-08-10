package org.systemsbiology.addama.services.execution.args;

import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategy.Strategy;

import java.util.Map;

import static org.systemsbiology.addama.services.execution.args.ArgumentStrategy.Strategy.valueOf;

/**
 * @author hrovira
 */
public class ArgumentStrategyJsonConfigHandler extends GenericMapJsonConfigHandler<ArgumentStrategy> {

    public ArgumentStrategyJsonConfigHandler(Map<String, ArgumentStrategy> map) {
        super(map);
    }

    public ArgumentStrategy getSpecific(JSONObject item) throws JSONException {
        if (item.has("argumentStrategy")) {
            Strategy strategy = valueOf(item.getString("argumentStrategy"));
            switch (strategy) {
                case formDataAsFile:
                    return new FormDataAsFileArgumentStrategy();
                case parseScriptVars:
                    return new ParseScriptVarsArgumentStrategy();
            }
        }
        return new DefaultArgumentStrategy();
    }
}

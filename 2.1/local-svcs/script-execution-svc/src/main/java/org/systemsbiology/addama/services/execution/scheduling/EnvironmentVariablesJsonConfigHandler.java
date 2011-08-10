package org.systemsbiology.addama.services.execution.scheduling;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author hrovira
 */
public class EnvironmentVariablesJsonConfigHandler implements JsonConfigHandler {
    private final Collection<String> variables;

    public EnvironmentVariablesJsonConfigHandler(Collection<String> variables) {
        this.variables = variables;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("environmentVars")) {
            JSONObject jsonVars = configuration.getJSONObject("environmentVars");

            Iterator keys = jsonVars.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                variables.add(key + "=" + jsonVars.getString(key));
            }
        }

    }
}

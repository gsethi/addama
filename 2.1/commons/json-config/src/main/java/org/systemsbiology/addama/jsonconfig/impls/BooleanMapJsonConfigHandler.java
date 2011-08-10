package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author hrovira
 */
public class BooleanMapJsonConfigHandler extends GenericMapJsonConfigHandler<Boolean> {
    public BooleanMapJsonConfigHandler(Map<String, Boolean> map, String name) {
        super(map, name);
    }

    public Boolean getSpecific(JSONObject item) throws JSONException {
        if (item != null && item.has(propertyName)) {
            return item.getBoolean(propertyName);
        }
        return null;
    }
}

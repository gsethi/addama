package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author hrovira
 */
public class IntegerMapJsonConfigHandler extends GenericMapJsonConfigHandler<Integer> {
    public IntegerMapJsonConfigHandler(Map<String, Integer> map, String name) {
        super(map, name);
    }

    public Integer getSpecific(JSONObject item) throws JSONException {
        if (item != null && item.has(propertyName)) {
            return item.getInt(propertyName);
        }
        return null;
    }
}

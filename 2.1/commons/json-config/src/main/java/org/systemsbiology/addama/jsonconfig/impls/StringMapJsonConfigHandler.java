package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author hrovira
 */
public class StringMapJsonConfigHandler extends GenericMapJsonConfigHandler<String> {
    public StringMapJsonConfigHandler(Map<String, String> map, String name) {
        super(map, name);
    }

    public String getSpecific(JSONObject item) throws JSONException {
        if (item != null && item.has(propertyName)) {
            return item.getString(propertyName);
        }
        return null;
    }
}

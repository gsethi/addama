package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.jsonconfig.JsonConfigHandler.Keys.locals;
import static org.systemsbiology.addama.jsonconfig.JsonConfigHandler.Keys.uri;

/**
 * @author hrovira
 */
public abstract class GenericMapJsonConfigHandler<T> implements JsonConfigHandler {
    private final Map<String, T> propertiesByUri;
    protected final String propertyName;

    public GenericMapJsonConfigHandler(Map<String, T> map) {
        this(map, null);
    }

    public GenericMapJsonConfigHandler(Map<String, T> map, String propertyName) {
        this.propertiesByUri = map;
        this.propertyName = propertyName;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has(locals.name())) {
            JSONArray array = configuration.getJSONArray(locals.name());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                if (item != null && item.has(uri.name())) {
                    if (isEmpty(propertyName)) {
                        propertiesByUri.put(item.getString(uri.name()), getSpecific(item));
                    } else if (item.has(propertyName)) {
                        propertiesByUri.put(item.getString(uri.name()), getSpecific(item));
                    }
                }
            }
        }
    }

    public abstract T getSpecific(JSONObject item) throws Exception;

}

package org.systemsbiology.addama.jsonconfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author hrovira
 */
public final class Mapping {
    private final String id;
    private final String label;
    private final JSONObject mapping;

    public Mapping(JSONObject mapping) throws JSONException {
        this.mapping = mapping;
        this.id = this.mapping.getString("id");
        this.label = this.mapping.getString("label");
    }

    public String ID() {
        return this.id;
    }

    public String LABEL() {
        return this.label;
    }

    public JSONObject JSON() {
        return this.mapping;
    }
}

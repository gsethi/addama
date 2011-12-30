package org.systemsbiology.addama.jsonconfig;

import org.json.JSONException;
import org.json.JSONObject;

import static org.apache.commons.lang.StringUtils.chomp;

/**
 * @author hrovira
 */
public final class Mapping {
    private final String id;
    private final String label;
    private final String uri;
    private final JSONObject mapping;

    public Mapping(String base, JSONObject mapping) throws JSONException {
        this.mapping = mapping;
        this.id = this.mapping.getString("id");
        this.label = this.mapping.getString("label");
        this.uri = chomp(this.mapping.optString("uri", base), "/");
    }

    public String ID() {
        return this.id;
    }

    public String LABEL() {
        return this.label;
    }

    public String URI() {
        return this.uri;
    }

    public JSONObject JSON() {
        return this.mapping;
    }
}

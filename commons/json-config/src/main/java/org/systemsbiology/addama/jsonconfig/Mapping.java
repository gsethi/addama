package org.systemsbiology.addama.jsonconfig;

import org.json.JSONObject;

import static org.apache.commons.lang.StringUtils.chomp;

/**
 * @author hrovira
 */
public final class Mapping {
    public final String id;
    public final String label;
    public final String base;
    private final JSONObject mapping;

    public Mapping(String id, String label, String base, JSONObject mapping) {
        this.id = id;
        this.label = label;
        this.base = chomp(base, "/");

        if (mapping == null) mapping = new JSONObject();
        this.mapping = mapping;
    }

    public String URI() {
        return this.base + "/" + this.id;
    }

    public JSONObject JSON() {
        return this.mapping;
    }
}

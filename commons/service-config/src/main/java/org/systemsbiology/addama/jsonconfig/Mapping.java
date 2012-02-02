package org.systemsbiology.addama.jsonconfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public final class Mapping {
    private final String id;
    private final String label;
    private final JSONObject mapping;
    private final HashSet<String> writers = new HashSet<String>();

    public Mapping(JSONObject mapping) throws JSONException {
        this.mapping = mapping;
        this.id = this.mapping.getString("id");
        this.label = this.mapping.getString("label");
        this.append("writers", writers);
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

    public Iterable<String> Writers() {
        return this.writers;
    }

    public boolean hasWriters() {
        return !this.writers.isEmpty();
    }

    public boolean isWriter(String email) {
        return this.writers.contains(email);
    }

    /*
    * Private Methods
    */
    private void append(String key, HashSet<String> set) throws JSONException {
        if (this.mapping.has(key)) {
            JSONArray readersArray = this.mapping.getJSONArray(key);
            for (int i = 0; i < readersArray.length(); i++) {
                String reader = readersArray.getString(i);
                if (!isEmpty(reader)) {
                    set.add(reader);
                }
            }
        }
    }
}

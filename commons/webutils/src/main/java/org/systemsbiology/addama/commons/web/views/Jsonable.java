package org.systemsbiology.addama.commons.web.views;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author hrovira
 */
public interface Jsonable {

    public JSONObject toJSON() throws JSONException;

}

package org.systemsbiology.addama.appengine.editors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.systemsbiology.addama.commons.web.editors.JSONArrayPropertyEditor;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class JSONArrayPropertyEditorTest {
    private static final String JSON_ARRAY = "[{user:'hector',membership:'member'}]";

    private JSONArrayPropertyEditor editor = new JSONArrayPropertyEditor();

    @Test
    public void validValue() throws JSONException {
        editor.setAsText(JSON_ARRAY);

        JSONArray jsonArray = (JSONArray) editor.getValue();
        assertNotNull(jsonArray);
        assertEquals(1, jsonArray.length());

        JSONObject jsonObject = jsonArray.getJSONObject(0);
        assertNotNull(jsonObject);
        assertTrue(jsonObject.has("user"));
        assertTrue(jsonObject.has("membership"));
        assertEquals("hector", jsonObject.getString("user"));
        assertEquals("member", jsonObject.getString("membership"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullValue() throws JSONException {
        editor.setAsText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyValue() throws JSONException {
        editor.setAsText("");
    }
}

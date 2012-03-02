package org.systemsbiology.addama.commons.web.editors;

import org.json.JSONArray;
import org.json.JSONException;

import java.beans.PropertyEditorSupport;

/**
 * @author hrovira
 */
public class JSONArrayPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        try {
            setValue(new JSONArray(text));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

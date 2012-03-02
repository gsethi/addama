package org.systemsbiology.addama.commons.web.editors;

import org.json.JSONException;
import org.json.JSONObject;

import java.beans.PropertyEditorSupport;

/**
 * @author hrovira
 */
public class JSONObjectPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        try {
            setValue(new JSONObject(text));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

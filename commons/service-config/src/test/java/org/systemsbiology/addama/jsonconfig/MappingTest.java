package org.systemsbiology.addama.jsonconfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class MappingTest {

    @Test
    public void simple() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", "id");
        json.put("label", "label");

        Mapping m = new Mapping(json);
        assertNotNull(m.ID());
        assertNotNull(m.LABEL());
        assertEquals("id", m.ID());
        assertEquals("label", m.LABEL());
        assertFalse(m.hasWriters());
        assertFalse(m.isWriter("user@addama.org"));
        assertFalse(m.Writers().iterator().hasNext());
    }

    @Test(expected = JSONException.class)
    public void noid() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("label", "label");
        new Mapping(json);
    }

    @Test(expected = JSONException.class)
    public void nolabel() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", "id");
        new Mapping(json);
    }

    @Test
    public void writers() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", "id");
        json.put("label", "label");
        json.append("writers", "writer@addama.org");

        Mapping m = new Mapping(json);
        assertNotNull(m.ID());
        assertNotNull(m.LABEL());
        assertEquals("id", m.ID());
        assertEquals("label", m.LABEL());

        assertTrue(m.hasWriters());
        assertTrue(m.Writers().iterator().hasNext());
        assertFalse(m.isWriter("reader@addama.org"));
        assertTrue(m.isWriter("writer@addama.org"));
    }
}

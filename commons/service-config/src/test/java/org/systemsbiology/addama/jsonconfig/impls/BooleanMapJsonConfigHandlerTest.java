package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class BooleanMapJsonConfigHandlerTest {
    private final static String PROPERTY = "PROPERTY";
    private HashMap<String, Boolean> map;
    private BooleanMapJsonConfigHandler handler;

    @Before
    public void setup() throws Exception {
        JSONObject json = new JSONObject();
        json.append("locals", new JSONObject().put("uri", "expected_true").put(PROPERTY, true));
        json.append("locals", new JSONObject().put("uri", "expected_false").put(PROPERTY, false));
        json.append("locals", new JSONObject().put("uri", "expected_false_null"));

        map = new HashMap<String, Boolean>();
        handler = new BooleanMapJsonConfigHandler(map, PROPERTY);
        handler.handle(json);
    }

    @Test
    public void checkValues() {
        assertTrue(map.get("expected_true"));
        assertFalse(map.get("expected_false"));
        assertFalse(map.containsKey("expected_false_null"));
        assertNull(map.get("expected_false_null"));
    }

}

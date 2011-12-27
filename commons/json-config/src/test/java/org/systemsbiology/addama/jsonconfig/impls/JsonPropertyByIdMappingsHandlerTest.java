package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class JsonPropertyByIdMappingsHandlerTest {
    private final Map<String, JSONObject> noDefaults = new HashMap<String, JSONObject>();
    private final Map<String, JSONObject> withDefaults = new HashMap<String, JSONObject>();
    private ServiceConfig serviceConfig;

    @Before
    public void setup() throws Exception {
        serviceConfig = new ServiceConfig(new ClassPathResource("testservice.config"));

        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(noDefaults, "goodJson"));

        JSONObject defaultJson = new JSONObject().put("v", "DEFAULT_VALUE");
        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(withDefaults, "goodJson", defaultJson));
    }

    @Test
    public void test_json_good() throws JSONException {
        assertNotNull(noDefaults.get("test_json_good"));
        assertEquals("inside_value", noDefaults.get("test_json_good").getString("v"));
        assertEquals("inside_value", withDefaults.get("test_json_good").getString("v"));
    }

    @Test
    public void test_json_absent() throws JSONException {
        assertFalse(noDefaults.containsKey("test_json_absent"));
        assertNull(noDefaults.get("test_json_absent"));

        assertTrue(withDefaults.containsKey("test_json_absent"));
        assertEquals("DEFAULT_VALUE", withDefaults.get("test_json_absent").getString("v"));
    }

    @Test(expected = JSONException.class)
    public void test_json_array() throws Exception {
        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(new HashMap<String, JSONObject>(), "arrayJson"));
    }

    @Test(expected = JSONException.class)
    public void test_json_bad() throws Exception {
        serviceConfig.visit(new JsonPropertyByIdMappingsHandler(new HashMap<String, JSONObject>(), "badJson"));
    }
}

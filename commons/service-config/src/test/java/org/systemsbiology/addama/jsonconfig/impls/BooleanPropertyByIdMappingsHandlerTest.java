package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONException;
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
public class BooleanPropertyByIdMappingsHandlerTest {
    private final Map<String, Boolean> noDefaults = new HashMap<String, Boolean>();
    private final Map<String, Boolean> withDefaults = new HashMap<String, Boolean>();
    private ServiceConfig serviceConfig;

    @Before
    public void setup() throws Exception {
        serviceConfig = new ServiceConfig(new ClassPathResource("testservice.config"));
        serviceConfig.visit(new BooleanPropertyByIdMappingsHandler(noDefaults, "goodBoolean"));
        serviceConfig.visit(new BooleanPropertyByIdMappingsHandler(withDefaults, "goodBoolean", true));
    }

    @Test
    public void test_boolean_true() {
        assertTrue(noDefaults.get("test_boolean_true"));
        assertTrue(withDefaults.get("test_boolean_true"));
    }

    @Test
    public void test_boolean_false() {
        assertFalse(noDefaults.get("test_boolean_false"));
        assertFalse(withDefaults.get("test_boolean_false"));
    }

    @Test
    public void test_boolean_as_string() {
        assertTrue(noDefaults.get("test_boolean_as_string"));
        assertTrue(withDefaults.get("test_boolean_as_string"));
    }

    @Test
    public void test_boolean_absent() {
        assertFalse(noDefaults.containsKey("test_boolean_absent"));
        assertNull(noDefaults.get("test_boolean_absent"));

        assertTrue(withDefaults.containsKey("test_boolean_absent"));
        assertTrue(withDefaults.get("test_boolean_absent"));
    }

    @Test(expected = JSONException.class)
    public void test_boolean_bad() throws Exception {
        serviceConfig.visit(new BooleanPropertyByIdMappingsHandler(new HashMap<String, Boolean>(), "badBoolean"));
    }

}

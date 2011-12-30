package org.systemsbiology.addama.jsonconfig.impls;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class StringPropertyByIdMappingsHandlerTest {
    private final Map<String, String> noDefaults = new HashMap<String, String>();
    private final Map<String, String> withDefaults = new HashMap<String, String>();

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("testservice");

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setServletContext(msc);
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(noDefaults, "goodString"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(withDefaults, "goodString", "DEFAULT_VALUE"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(noDefaults, "badString"));
    }

    @Test
    public void test_string_good() {
        assertNotNull(noDefaults.get("test_string_good"));
        assertEquals("string_value", noDefaults.get("test_string_good"));
        assertEquals("string_value", withDefaults.get("test_string_good"));
    }

    @Test
    public void test_string_absent() {
        assertFalse(noDefaults.containsKey("test_string_absent"));
        assertNull(noDefaults.get("test_string_absent"));

        assertTrue(withDefaults.containsKey("test_string_absent"));
        assertEquals("DEFAULT_VALUE", withDefaults.get("test_string_absent"));
    }

    @Test
    public void test_string_empty() {
        assertTrue(noDefaults.containsKey("test_string_empty"));
        assertEquals("", noDefaults.get("test_string_empty"));

        assertTrue(withDefaults.containsKey("test_string_empty"));
        assertEquals("", withDefaults.get("test_string_empty"));
    }

    @Test
    public void test_string_as_boolean() throws Exception {
        assertTrue(noDefaults.containsKey("test_string_as_boolean"));
        assertEquals("true", noDefaults.get("test_string_as_boolean"));
    }
}

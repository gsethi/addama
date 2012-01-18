package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class IntegerPropertyByIdMappingsHandlerTest {
    private final Map<String, Integer> noDefaults = new HashMap<String, Integer>();
    private final Map<String, Integer> withDefaults = new HashMap<String, Integer>();
    private ServiceConfig serviceConfig;

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("testservice");
        serviceConfig = new ServiceConfig();
        serviceConfig.setServletContext(msc);
        serviceConfig.visit(new IntegerPropertyByIdMappingsHandler(noDefaults, "goodInteger"));
        serviceConfig.visit(new IntegerPropertyByIdMappingsHandler(withDefaults, "goodInteger", 42));
    }

    @Test
    public void test_integer_good() {
        assertNotNull(noDefaults.get("test_integer_good"));
        assertSame(33, noDefaults.get("test_integer_good"));
        assertNotNull(withDefaults.get("test_integer_good"));
        assertSame(33, withDefaults.get("test_integer_good"));
    }

    @Test
    public void test_integer_absent() {
        assertFalse(noDefaults.containsKey("test_integer_absent"));
        assertTrue(withDefaults.containsKey("test_integer_absent"));
        assertSame(42, withDefaults.get("test_integer_absent"));
    }

    @Test
    public void test_integer_as_string() {
        assertNotNull(noDefaults.get("test_integer_as_string"));
        assertSame(54, noDefaults.get("test_integer_as_string"));
        assertNotNull(withDefaults.get("test_integer_as_string"));
        assertSame(54, withDefaults.get("test_integer_as_string"));
    }

    @Test(expected = JSONException.class)
    public void test_integer_bad() throws Exception {
        serviceConfig.visit(new IntegerPropertyByIdMappingsHandler(new HashMap<String, Integer>(), "badInteger"));
    }

}

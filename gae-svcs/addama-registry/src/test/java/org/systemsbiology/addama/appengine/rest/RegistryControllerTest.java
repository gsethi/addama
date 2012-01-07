package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.systemsbiology.addama.appengine.pojos.RegistryMapping;
import org.systemsbiology.addama.appengine.pojos.RegistryService;

import java.util.HashMap;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.*;
import static org.systemsbiology.addama.appengine.util.Registry.getRegistryMappings;
import static org.systemsbiology.addama.appengine.util.Registry.getRegistryService;


/**
 * @author hrovira
 */
public class RegistryControllerTest {
    private LocalServiceTestHelper HELPER;
    private RegistryController CONTROLLER;

    @Before
    public void setUp() throws Exception {
        HELPER = new LocalServiceTestHelper(new LocalUserServiceTestConfig(),
                new LocalMemcacheServiceTestConfig(), new LocalDatastoreServiceTestConfig());
        HELPER.setEnvEmail("admin@addama.org");
        HELPER.setEnvIsLoggedIn(true);
        HELPER.setEnvAuthDomain("addama.org");
        HELPER.setEnvIsAdmin(true);
        HELPER.setUp();

        CONTROLLER = new RegistryController();
    }

    @After
    public void tearDown() throws Exception {
        if (HELPER != null) {
            HELPER.tearDown();
        }
    }

    @Test
    public void registration() throws Exception {
        JSONObject json = new JSONObject();
        json.put("url", "https://example.org/service");
        json.put("label", "label");
        json.put("family", "/addama/family");
        for (int i = 0; i < 3; i++) {
            json.append("mappings", new JSONObject().put("id", "mapping_" + i).put("label", "label " + i));
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        CONTROLLER.register(new MockHttpServletRequest(), response, json);

        assertTrue(response.getStatus() == SC_OK);

        String registryKey = response.getHeader("x-addama-registry-key");
        assertNotNull(registryKey);

        RegistryService rs = getRegistryService("example.org.service");
        assertNotNull(rs);
        assertEquals(registryKey, rs.getAccessKey().toString());
        assertEquals("label", rs.getLabel());
        assertEquals("example.org.service", rs.getUri());
        assertEquals("https://example.org/service", rs.getUrl().toString());
        assertFalse(rs.isSearchable());

        Iterable<RegistryMapping> mappings = getRegistryMappings(rs);
        HashMap<String, String> labelsByUri = new HashMap<String, String>();
        assertNotNull(mappings);
        for (RegistryMapping rm : mappings) {
            assertNotNull(rm.getServiceUri());
            assertEquals("example.org.service", rm.getServiceUri());
            assertTrue(rm.getUri().startsWith("/addama/family"));
            labelsByUri.put(rm.getUri(), rm.getLabel());
        }

        for (int i = 0; i < 3; i++) {
            String actualUri = "/addama/family/mapping_" + i;
            assertTrue(labelsByUri.containsKey(actualUri));
            String actualLabel = labelsByUri.get(actualUri);
            assertNotNull(actualLabel);
            assertEquals("label " + i, labelsByUri.get(actualUri));
        }
    }

}

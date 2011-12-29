package org.systemsbiology.addama.jsonconfig.impls;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class CollectIdsMappingsHandlerTest {
    private final HashSet<String> ids = new HashSet<String>();
    private ServiceConfig serviceConfig;

    @Before
    public void setup() throws Exception {
        serviceConfig = new ServiceConfig(new ClassPathResource("testservice.config"));
        serviceConfig.visit(new CollectIdsMappingsHandler(ids));
    }

    @Test
    public void test_collected_any() throws JSONException {
        assertFalse(ids.isEmpty());
    }

    @Test
    public void test_verify_collected() throws JSONException {
        JSONArray mappings = serviceConfig.JSON().getJSONArray("mappings");
        assertEquals(mappings.length(), ids.size());
        for (int i = 0; i < mappings.length(); i++) {
            assertTrue(ids.contains(mappings.getJSONObject(i).getString("id")));
        }
    }

}

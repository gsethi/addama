package org.systemsbiology.addama.fsutils.controllers.workspaces;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class SchemaControllerTest {
    private MockHttpServletRequest request;
    private SchemaController controller;

    @Before
    public void setup() throws Exception {
        controller = new SchemaController();
        controller.setServiceConfig(new ServiceConfig(new ClassPathResource("schemaControllerTest.config")));

        request = new MockHttpServletRequest("get", "/addama/repositories/repo1/dataset.tsv/schema");
    }

    @Test
    public void simple() throws Exception {
        ModelAndView mav = controller.schema(request, "repo1");
        assertNotNull(mav);
        assertNotNull(mav.getModel());

        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertTrue(json.has("items"));

        JSONArray items = json.getJSONArray("items");
        assertNotNull(items);
        assertEquals(5, items.length());

        HashMap<String, String> expectedValues = new HashMap<String, String>();
        expectedValues.put("column a", "string");
        expectedValues.put("column b", "number");
        expectedValues.put("column c", "number");
        expectedValues.put("column d", "boolean");
        expectedValues.put("column e", "string");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            assertNotNull(item);
            assertTrue(item.has("name"));
            assertTrue(item.has("datatype"));

            String actualName = item.getString("name");
            assertTrue(expectedValues.containsKey(actualName));

            String expectedType = expectedValues.get(actualName);
            assertNotNull(expectedType);

            assertEquals(actualName, expectedType, item.getString("datatype"));
        }
    }
}

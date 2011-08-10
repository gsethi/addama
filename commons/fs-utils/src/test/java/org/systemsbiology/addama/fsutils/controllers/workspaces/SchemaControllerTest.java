package org.systemsbiology.addama.fsutils.controllers.workspaces;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;

import java.io.File;
import java.io.FileOutputStream;
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
        String uri = "/a/w/schemacontroller";
        String rootPath = "target/test-classes/test-SchemaControllerTest";
        String filename = "dataset.tsv";

        File dir = new File(rootPath + uri);
        dir.mkdirs();

        FileOutputStream fos = new FileOutputStream(new File(dir, filename));
        fos.write(testOutput().getBytes());
        fos.close();

        JSONObject local = new JSONObject().put("uri", uri).put("rootPath", rootPath);

        controller = new SchemaController();
        controller.setJsonConfig(new JsonConfig(new JSONObject().append("locals", local)));

        request = new MockHttpServletRequest("get", uri + "/" + filename + "/schema");
    }

    @Test
    public void simple() throws Exception {
        ModelAndView mav = controller.schema(request);
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

    /*
     * Private Methods
     */

    private String testOutput() {
        StringBuilder builder = new StringBuilder();
        builder.append("column a").append("\t");
        builder.append("column b").append("\t");
        builder.append("column c").append("\t");
        builder.append("column d").append("\t");
        builder.append("column e").append("\n");

        builder.append("test a start").append("\t");
        builder.append("12.34").append("\t");
        builder.append("1234").append("\t");
        builder.append("true").append("\t");
        builder.append("test a end").append("\n");

        builder.append("test b start").append("\t");
        builder.append("12.34").append("\t");
        builder.append("1234").append("\t");
        builder.append("false").append("\t");
        builder.append("test b end").append("\n");

        return builder.toString();
    }

}

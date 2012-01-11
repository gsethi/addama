package org.systemsbiology.addama.workspaces.fs.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class MainControllerTest {
    private MainController controller;

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("mainControllerTest");

        ServiceConfig config = new ServiceConfig();
        config.setServletContext(msc);

        controller = new MainController();
        controller.setServiceConfig(config);
    }

    @Test
    public void valid_cases() throws Exception {
        assertResource("repo_1", controller.getTargetResource("repo_1", ""));
        assertResource("some_file.txt", controller.getTargetResource("repo_1", "/some_dir/some_file.txt"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void null_repository() throws Exception {
        controller.getTargetResource(null, null);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void unknown_repository() throws Exception {
        controller.getTargetResource("unknown", null);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void invalid_rootpath() throws Exception {
        controller.getTargetResource("unknown", "some_dir");
    }

    @Test
    public void schema() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("get", "/addama/workspaces/repo_0/dataset.tsv/schema");
        ModelAndView mav = controller.schema(request, "repo_0");
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

    @Test(expected = ResourceNotFoundException.class)
    public void check_dir_notthere() throws Exception {
        controller.get(new MockHttpServletRequest("get", "/addama/workspaces/repo_1/dir_notthere"), "repo_1");
    }

    @Test
    public void add_dir() throws Exception {
        String newDirUri = "/addama/workspaces/repo_1/dir_one";

        controller.post(new MockHttpServletRequest("post", newDirUri), "repo_1");

        ModelAndView mav = controller.get(new MockHttpServletRequest("get", newDirUri), "repo_1");
        assertNotNull(mav);
        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertNotNull(json.getString("uri"));
        assertEquals(newDirUri, json.getString("uri"));
    }

    /*
    * Private Methods
    */

    private void assertResource(String expectedFilename, Resource someFile) throws ResourceNotFoundException, IOException {
        assertNotNull(someFile);
        assertNotNull(someFile.getFile());
        assertEquals(expectedFilename, someFile.getFilename());
        assertTrue(someFile.getFile().exists());
    }

}

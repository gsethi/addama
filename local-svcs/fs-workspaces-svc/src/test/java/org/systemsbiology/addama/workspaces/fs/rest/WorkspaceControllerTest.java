package org.systemsbiology.addama.workspaces.fs.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ReadOnlyAccessException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.junit.Assert.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.pipe;
import static org.systemsbiology.addama.commons.web.views.JsonItemsFromFilesView.URI;
import static org.systemsbiology.addama.commons.web.views.ResourceFileView.RESOURCE;

/**
 * @author hrovira
 */
public class WorkspaceControllerTest {
    private WorkspaceController controller;

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("mainControllerTest");

        ServiceConfig config = new ServiceConfig();
        config.setServletContext(msc);

        controller = new WorkspaceController();
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
        controller.resource(new MockHttpServletRequest("get", "/addama/workspaces/repo_1/dir_notthere"), "repo_1");
    }

    @Test
    public void add_dir() throws Exception {
        String newDirUri = "/addama/workspaces/repo_1/dir_one";

        controller.update(new MockHttpServletRequest("post", newDirUri), "repo_1");

        ModelAndView mav = controller.resource(new MockHttpServletRequest("get", newDirUri), "repo_1");
        assertNotNull(mav);
        String uri = (String) mav.getModel().get(URI);
        assertNotNull(uri);
        assertEquals(newDirUri, uri);
    }

    @Test
    public void add_file() throws Exception {
        String newDirUri = "/addama/workspaces/repo_1/dir_one";
        String newFileName = "file_one.txt";
        String someContent = "some content";

        MockHttpServletRequest request = new MockHttpServletRequest("post", newDirUri);
        addMultipartContent(request, new MockMultipartFile(newFileName, someContent.getBytes()));
        assertTrue(isMultipartContent(request));

        ModelAndView mav = controller.update(request, "repo_1");

        assertNotNull(mav);
        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);

        String uri = json.getString("uri");
        assertNotNull(uri);
        assertEquals(newDirUri, uri);

        JSONArray items = json.getJSONArray("items");
        assertNotNull(items);
        assertEquals(1, items.length());

        JSONObject item = items.getJSONObject(0);
        assertNotNull(item);
        assertNotNull(item.getString("name"));
        assertEquals(newFileName, item.getString("name"));

        ModelAndView getmav = controller.resource(new MockHttpServletRequest("get", newDirUri + "/" + newFileName), "repo_1");
        assertNotNull(getmav);
        Resource resource = (Resource) getmav.getModel().get(RESOURCE);
        assertNotNull(resource);

        InputStream is = resource.getInputStream();
        assertNotNull(is);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pipe(is, baos);
        assertEquals(someContent, new String(baos.toByteArray()));
    }

    @Test(expected = ReadOnlyAccessException.class)
    public void wi_writers() throws Exception {
        controller.update(new MockHttpServletRequest("POST", "/addama/workspaces/repo_wi_writers/writable_dir"), "repo_wi_writers");
    }

    @Test
    public void wo_writers() throws Exception {
        controller.update(new MockHttpServletRequest("POST", "/addama/workspaces/repo_wo_writers/writable_dir"), "repo_wo_writers");
    }

    @Test
    public void is_writer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/addama/workspaces/repo_wi_writers/writable_dir");
        request.addHeader("x-addama-registry-user", "writer1@addama.org");
        controller.update(request, "repo_wi_writers");
    }

    @Test(expected = ReadOnlyAccessException.class)
    public void isnot_writer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/addama/workspaces/repo_wi_writers/writable_dir");
        request.addHeader("x-addama-registry-user", "writer3@addama.org");
        controller.update(request, "repo_wi_writers");
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

    private void addMultipartContent(MockHttpServletRequest request, MockMultipartFile... multipartFiles) throws IOException {
        final String boundary = "simple boundary";

        StringBuilder builder = new StringBuilder(128);
        for (MockMultipartFile multipartFile : multipartFiles) {
            builder.append("\r\n");
            builder.append("--");
            builder.append(boundary);
            builder.append("\r\n");
            builder.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
            builder.append(multipartFile.getName());
            builder.append("\r\n");
            builder.append("Content-Type:");
            builder.append(multipartFile.getContentType());
            builder.append("\r\n");
            builder.append("\r\n");
            builder.append(new String(multipartFile.getBytes()));
            builder.append("\r\n");
            builder.append("--");
            builder.append(boundary);
            builder.append("--");
            builder.append("\r\n");
        }
        request.setContentType("multipart/form-data; boundary=" + boundary);
        request.setContent(builder.toString().getBytes());
    }

}

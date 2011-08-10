package org.systemsbiology.addama.fsutils.controllers;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.JsonConfig;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class FileSystemControllerTest {
    private FileSystemController controller;

    @Before
    public void setup() throws Exception {
        JSONObject json = new JSONObject();
        json.append("locals", getLocal("repo_1", "some_dir", true));
        json.append("locals", getLocal("repo_2", "some_dir", false));
        json.append("locals", getLocal("repo_3", "some_dir", null));

        controller = new FileSystemController() {
        };
        controller.setJsonConfig(new JsonConfig(json));
    }

    @Test
    public void valid_cases() throws Exception {
        assertTrue(controller.allowsServingFiles("repo_1"));
        assertFalse(controller.allowsServingFiles("repo_2"));
        assertFalse(controller.allowsServingFiles("repo_3"));

        controller.assertServesFiles("repo_1");

        assertResource("some_dir", controller.getTargetResource("repo_1", ""));
        assertResource("some_file.txt", controller.getTargetResource("repo_1", "some_dir/some_file.txt"));

        assertResource("some_dir", controller.getTargetResource("repo_2", ""));
        assertResource("some_file.txt", controller.getTargetResource("repo_2", "some_dir/some_file.txt"));

        assertResource("some_dir", controller.getTargetResource("repo_3", ""));
        assertResource("some_file.txt", controller.getTargetResource("repo_3", "some_dir/some_file.txt"));
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

    @Test(expected = InvalidSyntaxException.class)
    public void not_serveFiles() throws Exception {
        assertFalse(controller.allowsServingFiles("repo_2"));
        controller.assertServesFiles("repo_2");
    }

    /*
    * Private Methods
    */

    private JSONObject getLocal(String uri, String rootPath, Boolean serveFiles) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("rootPath", rootPath);
        json.put("serveFiles", serveFiles);
        return json;
    }

    private void assertResource(String expectedFilename, Resource someFile) throws ResourceNotFoundException, IOException {
        assertNotNull(someFile);
        assertNotNull(someFile.getFile());
        assertEquals(expectedFilename, someFile.getFilename());
        assertFalse(someFile.getFile().exists());
    }
}

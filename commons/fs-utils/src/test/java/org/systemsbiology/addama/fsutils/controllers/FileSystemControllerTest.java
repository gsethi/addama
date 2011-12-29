package org.systemsbiology.addama.fsutils.controllers;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class FileSystemControllerTest {
    private FileSystemController controller;

    @Before
    public void setup() throws Exception {
        controller = new FileSystemController() {
        };
        controller.setServiceConfig(new ServiceConfig(new ClassPathResource("fileSystemControllerTest.config")));
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

package org.systemsbiology.addama.repositories.fs.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class MainControllerTest {
    private MainController controller;

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("testMainController");

        ServiceConfig config = new ServiceConfig();
        config.setServletContext(msc);

        controller = new MainController();
        controller.setServiceConfig(config);
    }

    @Test
    public void valid_assertServesFiles() throws Exception {
        controller.assertServesFiles("repo_1");
    }

    @Test(expected = InvalidSyntaxException.class)
    public void not_assertServesFiles() throws Exception {
        controller.assertServesFiles("repo_2");
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

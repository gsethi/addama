package org.systemsbiology.addama.fsutils.controllers.repositories;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

/**
 * @author hrovira
 */
public class AbstractRepositoriesControllerTest {
    private AbstractRepositoriesController controller;

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("abstractRepositoriesController");

        ServiceConfig config = new ServiceConfig();
        config.setServletContext(msc);

        controller = new AbstractRepositoriesController() {
        };
        controller.setServiceConfig(config);
    }

    @Test
    public void valid_cases() throws Exception {
        controller.assertServesFiles("repo_1");
    }

    @Test(expected = InvalidSyntaxException.class)
    public void not_serveFiles() throws Exception {
        controller.assertServesFiles("repo_2");
    }

}

package org.systemsbiology.addama.repositories.fs.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

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
    public void valid_cases() throws Exception {
        controller.assertServesFiles("repo_1");
    }

    @Test(expected = InvalidSyntaxException.class)
    public void not_serveFiles() throws Exception {
        controller.assertServesFiles("repo_2");
    }

}

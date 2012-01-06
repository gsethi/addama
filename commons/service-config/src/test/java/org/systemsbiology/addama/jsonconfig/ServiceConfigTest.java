package org.systemsbiology.addama.jsonconfig;

import org.junit.Test;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author hrovira
 */
public class ServiceConfigTest {
    @Test
    public void good() {
        ServiceConfig config = new ServiceConfig();
        config.setServletContext(getServletContext("testservice"));

        assertNotNull(config.JSON());
        assertNotNull(config.LABEL());
        assertEquals("Test Service", config.LABEL());
    }

    @Test
    public void bad() {
        ServiceConfig config = new ServiceConfig();
        config.setServletContext(getServletContext("notthere"));
        assertNull(config.JSON());
        assertNull(config.LABEL());
    }

    private MockServletContext getServletContext(String contextPath) {
        MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath(contextPath);
        return servletContext;
    }
}

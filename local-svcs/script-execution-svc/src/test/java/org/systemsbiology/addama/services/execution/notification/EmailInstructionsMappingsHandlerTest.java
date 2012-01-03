package org.systemsbiology.addama.services.execution.notification;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class EmailInstructionsMappingsHandlerTest {
    private final Map<String, EmailBean> emailBeansByUri = new HashMap<String, EmailBean>();
    private final EmailInstructionsMappingsHandler handler = new EmailInstructionsMappingsHandler(emailBeansByUri);

    @Before
    public void setup() throws Exception {
        MockServletContext msc = new MockServletContext();
        msc.setContextPath("emailInstructionsMappingsHandlerTest");
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setServletContext(msc);
        serviceConfig.visit(handler);
    }

    @Test
    public void normal() throws Exception {
        assertTrue(emailBeansByUri.containsKey("id"));

        EmailBean bean = emailBeansByUri.get("id");
        assertNotNull(bean);
        assertNotNull(bean.getFrom());
        assertNotNull(bean.getSubject());
        assertNotNull(bean.getMessage());
        assertNotNull(bean.getMailSender());

        assertEquals("email@addama.org", bean.getFrom());
        assertEquals("Subject", bean.getSubject());
        assertEquals("This is a sample email message", bean.getMessage().trim());
    }

    @Test
    public void noemail() throws Exception {
        assertFalse(emailBeansByUri.containsKey("noemail"));
        assertNull(emailBeansByUri.get("noemail"));
    }
}

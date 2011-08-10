package org.systemsbiology.addama.services.execution.notification;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class EmailJsonConfigHandlerTest {
    private static final String URI = "/addama/uri";

    private final Map<String, EmailBean> emailBeansByUri = new HashMap<String, EmailBean>();
    private final EmailJsonConfigHandler handler = new EmailJsonConfigHandler(emailBeansByUri);

    private File testEmailMsg;

    @Before
    public void setup() throws IOException {
        ClassPathResource resource = new ClassPathResource("email-json-config-handler-test/TestEmailMessage.txt");
        testEmailMsg = resource.getFile();
    }

    @Test
    public void normal() throws Exception {
        JSONObject emailJson = new JSONObject();
        emailJson.put("from", "email@addama.org");
        emailJson.put("subject", "Subject");
        emailJson.put("emailText", testEmailMsg.getAbsolutePath());
        emailJson.put("host", "localhost");

        JSONObject json = new JSONObject();
        json.put("uri", URI);
        json.put("emailInstructions", emailJson);

        handler.handle(new JSONObject().append("locals", json));

        EmailBean bean = emailBeansByUri.get(URI);
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
        JSONObject json = new JSONObject();
        json.put("uri", URI);

        handler.handle(new JSONObject().append("locals", json));

        assertFalse(emailBeansByUri.containsKey(URI));
        assertNull(emailBeansByUri.get(URI));
    }
}

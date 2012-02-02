package org.systemsbiology.addama.commons.web.utils;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;
import static org.systemsbiology.addama.commons.web.utils.RegisteredUser.getRegistryUser;

/**
 * @author hrovira
 */
public class RegisteredUserTest {
    private MockHttpServletRequest request;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
    }

    @Test
    public void wiuser() {
        request.addHeader("x-addama-registry-user", "registered@addama.org");

        String user = getRegistryUser(request);
        assertNotNull(user);
        assertEquals("registered@addama.org", user);
    }

    @Test
    public void wouser() {
        String user = getRegistryUser(request);
        assertNull(user);
    }
}

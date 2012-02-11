package org.systemsbiology.addama.commons.web.servlet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.systemsbiology.addama.commons.web.servlet.HttpRequestUriInvocationHandler.instrumentRequest;

/**
 * @author hrovira
 */
public class HttpRequestUriInvocationHandlerTest {
    private static final String ORIGINAL_URI = "ORIGINAL_URI";
    private static final String INSTRUMENTED_URI = "INSTRUMENTED_URI";
    private static final String ORIGINAL_CONTEXT_PATH = "ORIGINAL_CONTEXT_PATH";
    private MockHttpServletRequest request = new MockHttpServletRequest();

    @Before
    public void setup() {
        request.setRequestURI(ORIGINAL_URI);
        request.setContextPath(ORIGINAL_CONTEXT_PATH);
    }

    @Test
    public void instrument() {
        String originalUri = request.getRequestURI();
        assertNotNull(originalUri);
        assertEquals(ORIGINAL_URI, originalUri);

        String originalContextPath = request.getContextPath();
        assertNotNull(originalContextPath);
        assertEquals(ORIGINAL_CONTEXT_PATH, originalContextPath);

        HttpServletRequest instrumented = instrumentRequest(request, INSTRUMENTED_URI, null);
        String actualUri = instrumented.getRequestURI();
        assertNotNull(actualUri);
        assertEquals(INSTRUMENTED_URI, actualUri);

        String instrumentedContextPath = instrumented.getContextPath();
        assertNotNull(instrumentedContextPath);
        assertEquals(ORIGINAL_CONTEXT_PATH, instrumentedContextPath);
    }

    @Test
    public void header() {
        HttpServletRequest instrumented = instrumentRequest(request, INSTRUMENTED_URI, "user@addama.org");

        String user = instrumented.getHeader("x-addama-registry-user");
        assertNotNull(user);
        assertEquals("user@addama.org", user);
    }
}

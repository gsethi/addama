package org.systemsbiology.addama.registry;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.PassThroughFilterChain;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.springframework.mock.web.MockHttpServletResponse.SC_OK;
import static org.springframework.mock.web.MockHttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;

/**
 * @author hrovira
 */
public class RegistryServiceFilterTest {
    private static final Integer MAX_CONTENT_LEN = 10000000;

    private RegistryServiceFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setup() {
        filter = new RegistryServiceFilter();
        filter.registryServiceKeyByHost.put("host", "key");

        request = new MockHttpServletRequest("get", "/toolarge.file");
        request.addHeader("x-addama-registry-key", "key");
        request.addHeader("x-addama-registry-host", "host");

        response = new MockHttpServletResponse();
    }

    @Test
    public void zero() throws IOException, ServletException {
        doTest(SC_OK, new PassThroughFilterChain(filter, new SetContentMockFilterChain(0)));
        assertNull(response.getRedirectedUrl());
    }

    @Test
    public void less_than_max() throws IOException, ServletException {
        doTest(SC_OK, new PassThroughFilterChain(filter, new SetContentMockFilterChain(MAX_CONTENT_LEN - 100)));
        assertNull(response.getRedirectedUrl());
    }

    @Test
    public void equal_to_max() throws IOException, ServletException {
        doTest(SC_OK, new PassThroughFilterChain(filter, new SetContentMockFilterChain(MAX_CONTENT_LEN)));
        assertNull(response.getRedirectedUrl());
    }

    @Test
    public void more_than_max() throws IOException, ServletException {
        doTest(SC_REQUEST_ENTITY_TOO_LARGE, new PassThroughFilterChain(filter, new SetContentMockFilterChain(MAX_CONTENT_LEN + 100)));

        String location = (String) response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.startsWith("http://localhost:80/singlecall"));
    }

    @Test
    public void afdl() throws Exception {
        request.setRequestURI("/_afdl/anyfile.file");

        doTest(SC_REQUEST_ENTITY_TOO_LARGE, new PassThroughFilterChain(filter, new MockFilterChain()));

        String location = (String) response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.startsWith("http://localhost:80/singlecall"));
        assertTrue(location.endsWith("anyfile.file"));
    }

    /*
     * Private Methods
     */

    private void doTest(int expectedStatusCode, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(request, response);
        assertEquals(expectedStatusCode, response.getStatus());
    }
}

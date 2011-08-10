package org.systemsbiology.addama.commons.web.utils;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.junit.Assert.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;

/**
 * @author hrovira
 */
public class HttpIOTest {
    private static final String EXPECTED_CONTENT_TYPE = "application/expected";
    private static final String DEFAULT_CONTENT_TYPE = "application/default";

    private MockHttpServletRequest request;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
    }

    @Test
    public void valid_mimetype() {
        assertEquals("text/plain", getMimeType(request, "filename.txt"));
        assertEquals("text/html", getMimeType(request, "index.html"));
        assertEquals("image/jpeg", getMimeType(request, "image.jpg"));
        assertEquals("image/jpeg", getMimeType(request, "image.jpeg"));
    }

    @Test
    public void unknown_mimetype() {
        assertEquals("application/octet-stream", getMimeType(request, "file.unknown"));
        assertEquals("application/octet-stream", getMimeType(request, "file"));
    }

    @Test
    public void invalid_mimetype() {
        assertNull(getMimeType(request, ""));
        assertNull(getMimeType(request, (String) null));
    }

    @Test
    public void desired_contenttype_param() {
        request.setParameter(CONTENT_TYPE_HEADER_KEY, EXPECTED_CONTENT_TYPE);
        String contentType = getDesiredContentType(request, DEFAULT_CONTENT_TYPE);
        assertNotNull(contentType);
        assertEquals(EXPECTED_CONTENT_TYPE, contentType);
    }

    @Test
    public void desired_contenttype_header() {
        request.addHeader(CONTENT_TYPE_HEADER_KEY, EXPECTED_CONTENT_TYPE);

        String contentType = getDesiredContentType(request, DEFAULT_CONTENT_TYPE);
        assertNotNull(contentType);
        assertEquals(EXPECTED_CONTENT_TYPE, contentType);
    }

    @Test
    public void desired_contenttype_none() {
        String contentType = getDesiredContentType(request, DEFAULT_CONTENT_TYPE);
        assertNotNull(contentType);
        assertEquals(DEFAULT_CONTENT_TYPE, contentType);
    }

    @Test
    public void desired_contenttype_both() {
        request.setParameter(CONTENT_TYPE_HEADER_KEY, EXPECTED_CONTENT_TYPE + "_param");
        request.addHeader(CONTENT_TYPE_HEADER_KEY, EXPECTED_CONTENT_TYPE + "_header");

        String contentType = getDesiredContentType(request, DEFAULT_CONTENT_TYPE);
        assertNotNull(contentType);
        assertEquals(EXPECTED_CONTENT_TYPE + "_header", contentType);
        assertNotSame(EXPECTED_CONTENT_TYPE + "_param", contentType);
    }

    @Test
    public void desired_contenttype_invalid() {
        assertTrue(isEmpty(getDesiredContentType(request, null)));
        assertTrue(isEmpty(getDesiredContentType(request, "")));
    }

    @Test
    public void cleanSpaces_simple() {
        String cleanpath = cleanSpaces("/addama/workspaces/foo/bar/x%20y+z/que");
        assertNotNull(cleanpath);
        assertEquals("/addama/workspaces/foo/bar/x y z/que", cleanpath);
    }

    @Test
    public void cleanSpaces_noclean() {
        String cleanpath = cleanSpaces("/addama/workspaces/foo/bar/x yz/que");
        assertNotNull(cleanpath);
        assertEquals("/addama/workspaces/foo/bar/x yz/que", cleanpath);
    }

    @Test
    public void cleanSpaces_nospaces() {
        String cleanpath = cleanSpaces("/addama/workspaces/foo/bar/xyz/que");
        assertNotNull(cleanpath);
        assertEquals("/addama/workspaces/foo/bar/xyz/que", cleanpath);
    }

    @Test
    public void cleanSpaces_null() {
        assertNull(cleanSpaces(null));
    }

    @Test
    public void cleanUri_simple() {
        MockHttpServletRequest request = new MockHttpServletRequest("get", "/addama/workspaces/foo/bar/x%20y+z/que");
        String cleanpath = getCleanUri(request);
        assertNotNull(cleanpath);
        assertEquals("/addama/workspaces/foo/bar/x y z/que", cleanpath);
    }

    @Test
    public void cleanUri_noclean() {
        MockHttpServletRequest request = new MockHttpServletRequest("get", "/addama/workspaces/foo/bar/x yz/que");
        String cleanpath = getCleanUri(request);
        assertNotNull(cleanpath);
        assertEquals("/addama/workspaces/foo/bar/x yz/que", cleanpath);
    }

    @Test
    public void cleanUri_nospaces() {
        MockHttpServletRequest request = new MockHttpServletRequest("get", "/addama/workspaces/foo/bar/xyz/que");
        String cleanpath = getCleanUri(request);
        assertNotNull(cleanpath);
        assertEquals("/addama/workspaces/foo/bar/xyz/que", cleanpath);
    }

    @Test
    public void cleanUri_null() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String cleanUri = getCleanUri(request);
        assertTrue(isEmpty(cleanUri));
    }

}

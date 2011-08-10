package org.systemsbiology.addama.commons.web.views;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.junit.Assert.*;
import static org.springframework.mock.web.MockHttpServletResponse.SC_NOT_FOUND;
import static org.springframework.mock.web.MockHttpServletResponse.SC_OK;
import static org.systemsbiology.addama.commons.web.views.ResourceFileView.RESOURCE;

/**
 * @author hrovira
 */
public class ResourceFileViewTest {
    private ResourceFileView view;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Map<String, Object> model;

    @Before
    public void setup() {
        view = new ResourceFileView();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        model = new HashMap<String, Object>();
    }

    @Test
    public void valid() throws Exception {
        addFileToModel("example.txt");
        view.render(model, request, response);

        String content = response.getContentAsString();
        assertNotNull(content);
        assertEquals("this is a test file", content);

        assertContentType();
        assertContentDisposition("example.txt");
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void empty() throws Exception {
        addFileToModel("empty.txt");
        view.render(model, request, response);

        assertTrue(isEmpty(response.getContentAsString()));
        assertContentType();
        assertContentDisposition("empty.txt");
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void notfound() throws Exception {
        addFileToModel("notfound.txt");
        view.render(model, request, response);

        assertTrue(isEmpty(response.getContentAsString()));
        assertEquals(SC_NOT_FOUND, response.getStatus());
    }

    /*
    * Private Methods
    */

    private void addFileToModel(String fileclasspath) throws IOException {
        model.put(RESOURCE, new ClassPathResource("test-fileviews/" + fileclasspath));
    }

    private void assertContentDisposition(String filename) {
        String contentDisposition = (String) response.getHeader("Content-Disposition");
        assertNotNull(contentDisposition);
        assertEquals("attachment;filename=\"" + filename + "\"", contentDisposition);
    }

    private void assertContentType() {
        String contentType = response.getContentType();
        assertNotNull(contentType);
        assertEquals("text/plain", contentType);
    }
}

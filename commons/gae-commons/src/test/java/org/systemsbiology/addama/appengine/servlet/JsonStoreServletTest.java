package org.systemsbiology.addama.appengine.servlet;

import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.springframework.mock.web.MockHttpServletResponse.SC_OK;

/**
 * @author hrovira
 */
public class JsonStoreServletTest {
    private final JsonStoreServlet servlet = new JsonStoreServlet();
    private final MockHttpServletRequest GET = new MockHttpServletRequest();
    private final MockHttpServletRequest POST = new MockHttpServletRequest();

    private LocalServiceTestHelper helper;

    @Before
    public void setUp() {
        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
        helper.setUp();

        GET.setMethod("GET");
        POST.setMethod("POST");
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }

//    @Test

    public void parent_create() throws IOException, ServletException, JSONException {
        JSONObject post_json = new JSONObject();
        post_json.put("label", "test");

        POST.setRequestURI("/root/parents");
        POST.setContent(post_json.toString().getBytes());

        MockHttpServletResponse post_response = new MockHttpServletResponse();
        servlet.doPost(POST, post_response);

        assertEquals(SC_OK, post_response.getStatus());

        String post_content = post_response.getContentAsString();
        assertNotNull(post_content);

        JSONObject post_response_json = new JSONObject(post_content);
        assertNotNull(post_response_json);
        assertTrue(post_response_json.has("uri"));

        String post_response_uri = post_response_json.getString("uri");
        assertNotNull(post_response_uri);

        MockHttpServletResponse get_response = new MockHttpServletResponse();

        GET.setRequestURI(post_response_uri);
        servlet.doGet(GET, get_response);

        String get_content = get_response.getContentAsString();
        assertNotNull(get_content);

        JSONObject get_response_json = new JSONObject(get_content);
        assertNotNull(get_response_json);
        assertTrue(get_response_json.has("uri"));

        String get_response_uri = get_response_json.getString("uri");
        assertNotNull(get_response_uri);
        assertEquals(post_response_uri, get_response_uri);

        assertTrue(get_response_json.has("label"));
        String get_response_label = get_response_json.getString("label");
        assertNotNull(get_response_label);
        assertEquals("test", get_response_label);
    }

    @Test
    public void parent_retrieve() {

    }

    @Test
    public void parent_retrieve_children() {

    }

    @Test
    public void parent_update() {

    }

    @Test
    public void parent_delete() {

    }

    @Test
    public void child_create() {

    }

    @Test
    public void child_retrieve() {

    }

    @Test
    public void child_update() {

    }

    @Test
    public void child_delete() {

    }

}

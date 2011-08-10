package org.systemsbiology.addama.coresvcs.gae.asynch;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author hrovira
 */
public class CaptureOutputResponseTest {
    public static final String TEST_STRING = "{x:'y'}";
    public static final String TEST_CONTENT_TYPE = "application/json";
    private CaptureOutputResponse response;
    private HttpServletResponse mockResponse;

    @Before
    public void setup() {
        mockResponse = new MockHttpServletResponse();
        response = new CaptureOutputResponse(mockResponse);
    }

    @Test
    public void simple() throws Exception {
        PrintWriter pw = response.getWriter();
        pw.write(TEST_STRING);

        assertEquals(TEST_STRING, new String(response.getContent()));
    }

    @Test
    public void contentType() throws Exception {
        response.setContentType(TEST_CONTENT_TYPE);

        assertEquals(TEST_CONTENT_TYPE, response.getContentType());
        assertNull(mockResponse.getContentType());
    }
}

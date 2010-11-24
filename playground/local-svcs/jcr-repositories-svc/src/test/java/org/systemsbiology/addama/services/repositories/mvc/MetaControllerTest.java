/**
 * 
 */
package org.systemsbiology.addama.services.repositories.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;
import org.systemsbiology.addama.JcrTestHelper;

/**
 * Unit test suite for node meta controller which has the exact same functionality as the
 * GET method on the AnnotationsController
 * 
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 * 
 * @author deflaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"NodeCrudControllerTest-context.xml"})
public class MetaControllerTest {
	private static final Logger log = Logger
	.getLogger(MetaControllerTest.class.getName());

	private MockHttpServletRequest request = null;
	private MockHttpServletResponse response = null;
	private DispatcherServlet servlet = null;

	@Autowired
	private JcrTestHelper helper = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// Ensure that the JCR session remains open for the duration
		helper.obtainSession();

		// Add some test data to the JCR
		helper.createOrUpdateNode(
				"/TestData/TestNode",
                "{ 'aSingleWord':'same', 'anInteger':0, 'aBoolean':true, 'freeText':'stuff to be indexed for a free text search'}");

		// Get a handle to our servlet and wire up the jcrTemplate in 
		// the manner the controller expects
		servlet = helper.getDispatcherServlet();
		request = helper.getMockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.releaseSession();
	}

	/**
	 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.MetaController#meta(javax.servlet.http.HttpServletRequest)}.
	 * @throws Exception 
	 */
	@Test
	public final void testMeta() throws Exception {
		// Get meta (annotations) for a node
		request.setMethod("GET");
		request.setRequestURI("/path/TestData/TestNode/annotations");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		// Now check the structure of our response, it should look something like:
		// {
		//  "created-at":"11/23/2010 13:14:00",
		//  "anInteger":0,
		//  "freeText":"stuff to be indexed for a free text search",
		//  "this":"is an annotation",
		//  "aSingleWord":"same",
		//  "uri":"/path/TestData/TestNode",
		// }
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(true, results.getBoolean("aBoolean"));
		assertTrue(results.getString("created-at").matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
		assertEquals(0, results.getInt("anInteger"));
		assertEquals("stuff to be indexed for a free text search", results.getString("freeText"));
		assertEquals("same", results.getString("aSingleWord"));
		assertEquals("/path/TestData/TestNode", results.getString("uri"));
	}
}

/**
 * 
 */
package org.systemsbiology.addama.services.repositories.mvc;

import static org.junit.Assert.assertEquals;

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
 * Unit test suite for node "structured" controller which allows you to
 * retrieve the annotations for this node and all nodes below it.
 * 
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 * 
 * @author deflaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"NodeCrudControllerTest-context.xml"})
public class StructuredControllerTest {
	private static final Logger log = Logger
	.getLogger(StructuredControllerTest.class.getName());

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
				"/TestData/TestParentNode",
                "{ 'aSingleWord':'same', 'anInteger':0, 'aBoolean':true}");

		helper.createOrUpdateNode(
				"/TestData/TestParentNode/TestChildNode",
                "{ 'freeText':'stuff to be indexed for a free text search'}");

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
	 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.StructuredController#structured(javax.servlet.http.HttpServletRequest)}.
	 * @throws Exception 
	 */
	@Test
	public final void testStructured() throws Exception {
		// Get a node's descendents
		request.setMethod("GET");
		request.setRequestURI("/path/TestData/structured");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		// Now check the structure of our response, it should look something like:
		// {
		//  "TestParentNode":
		//    {
		//     "aBoolean":true,
		//     "TestChildNode":
		//       {
		//        "isFile":false,
		//        "created-at":"11/23/2010 15:00:59",
		//        "freeText":"stuff to be indexed for a free text search"
		//       },
		//     "isFile":false,
		//     "created-at":"11/23/2010 15:00:59",
		//     "anInteger":0,
		//     "aSingleWord":"same"
		//    }
		// }
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals("same", 
				results.getJSONObject("TestParentNode").getString("aSingleWord"));
		assertEquals("stuff to be indexed for a free text search", 
				results.getJSONObject("TestParentNode").getJSONObject("TestChildNode").getString("freeText"));
	}

}

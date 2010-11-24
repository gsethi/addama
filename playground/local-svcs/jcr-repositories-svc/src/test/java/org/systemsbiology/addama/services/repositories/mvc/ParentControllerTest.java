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
 * Unit test suite for node parent controller which allows you to retrieve the parent of anode.
 * 
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 * 
 * @author deflaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"NodeCrudControllerTest-context.xml"})
public class ParentControllerTest {
	private static final Logger log = Logger
	.getLogger(ParentControllerTest.class.getName());

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
	 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.ParentController#parent(javax.servlet.http.HttpServletRequest)}.
	 * @throws Exception 
	 */
	@Test
	public final void testParent() throws Exception {
		// Get a node's parent
		request.setMethod("GET");
		request.setRequestURI("/path/TestData/TestParentNode/TestChildNode/parent");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		// Now check the structure of our response, it should look something like:
		// {"name":"TestParentNode","uri":"/path/TestData/TestParentNode"}
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals("TestParentNode", results.getString("name"));
		assertEquals("/path/TestData/TestParentNode", results.getString("uri"));	
	}

	/**
	 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.ParentController#parent(javax.servlet.http.HttpServletRequest)}.
	 * @throws Exception 
	 */
	@Test
	public final void testParentIsRoot() throws Exception {
		// Get a node's parent
		request.setMethod("GET");
		request.setRequestURI("/path/TestData/parent");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		// Now check the structure of our response, it should look something like:
		// {"name":"root","uri":""}
		// Note that when we run this against the live system, it has the repository 
		// root as the uri.  For example {"name": "root","uri": "/addama/repositories/sage-content-repo"}
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals("root", results.getString("name"));
		assertEquals("", results.getString("uri"));	
	}
	
	/**
	 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.ParentController#parent(javax.servlet.http.HttpServletRequest)}.
	 * @throws Exception 
	 */
	@Test
	public final void testInvalidParent() throws Exception {
		// Get a node's parent
		request.setMethod("GET");
		request.setRequestURI("/path/parent");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we did not find it", 404, response.getStatus());
	}
}

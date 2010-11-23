/**
 *
 */
package org.systemsbiology.addama.services.repositories.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;
import org.systemsbiology.addama.JcrTestHelper;

/**
 * Unit test suite for node CRUD operations.
 * 
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 * 
 * Unfortunately I was not able to configure the DispatcherServlet with the
 * production configuration in files
 * src/main/webapp/WEB-INF/jcrrepos-servlet.xml and
 * src/main/webapp/WEB-INF/app-contexts/controllers.xml because the
 * org.systemsbiology.addama.jcr.support.JcrTemplateProvider bean does not play
 * nice in this unit test environment.
 * 
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class NodeCrudControllerTest {

	private static final Logger log = Logger
			.getLogger(NodeCrudControllerTest.class.getName());

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

		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status code.
		MockServletConfig servletConfig = new MockServletConfig("jcrrepos");
		servletConfig.addInitParameter("contextConfigLocation",	"/test-jcrrepos-servlet.xml");
		servlet = new DispatcherServlet();
		servlet.init(servletConfig);

		// Ensure that the JCR session remains open for the duration
		helper.obtainSession();

		// Add some test data to the JCR
		helper
				.createOrUpdateNode(
						"/TestData/TestNode1",
						"{ 'aSingleWord':'same', 'anInteger':0, 'aBoolean':true, 'freeText':'stuff to be indexed for a free text search'}");
		helper
				.createOrUpdateNode(
						"/TestData/TestNode2",
						"{ 'aSingleWord':'same', 'anInteger':5, 'aBoolean':false, 'freeText':'unit tests are handy'}");
		helper
				.createOrUpdateNode(
						"/TestData/TestNode3",
						"{ 'aSingleWord':'same', 'anInteger':-10, 'aBoolean':false, 'freeText':'unit tests are helpful'}");

		// Wire up the jcrTemplate in the manner the controller expects
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
	 * Test method for
	 * {@link org.systemsbiology.addama.services.repositories.mvc.NodeCrudController#get(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testGet() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/path/TestData/TestNode3");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());

		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue("we got the node we expected",
				response.getContentAsString().matches(".*unit tests are helpful.*"));
		
		// Now check the structure of our response, it should look something like:
		// {
		//  "aBoolean":false,
		//  "numberOfItems":0,
		//  "items":[],
		//  "isFile":false,
		//  "created-at":"11/15/2010 19:52:34",
		//  "anInteger":-10,
		//  "freeText":"unit tests are helpful",
		//  "aSingleWord":"same",
		//  "operations":{
		//     "directory":"/path/TestData/TestNode3/dir",
		//     "terms":"/path/TestData/TestNode3/annotations/terms",
		//     "annotations":"/path/TestData/TestNode3/annotations",
		//     "meta":"/path/TestData/TestNode3/meta"},
		//  "uri":"/path/TestData/TestNode3"
		//}

		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(false, results.getBoolean("aBoolean"));
		assertEquals(0, results.getInt("numberOfItems"));
		assertEquals(0, results.getJSONArray("items").length());
		assertEquals(false, results.getBoolean("isFile"));
		assertTrue(results.getString("created-at").matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
		assertEquals(-10, results.getInt("anInteger"));
		assertEquals("unit tests are helpful", results.getString("freeText"));
		assertEquals("same", results.getString("aSingleWord"));
		assertEquals("/path/TestData/TestNode3/dir", results.getJSONObject("operations").getString("directory"));
		assertEquals("/path/TestData/TestNode3/annotations", results.getJSONObject("operations").getString("annotations"));
		assertEquals("/path/TestData/TestNode3/annotations/terms", results.getJSONObject("operations").getString("terms"));
		assertEquals("/path/TestData/TestNode3/meta", results.getJSONObject("operations").getString("meta"));
		assertEquals("/path/TestData/TestNode3", results.getString("uri"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.services.repositories.mvc.NodeCrudController#post(javax.servlet.http.HttpServletRequest, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testPost() throws Exception {
		/**	
		 * Create a node
		 */
		request.setMethod("POST");
		request.setRequestURI("/path/TestData/TestNode4");
		request.setParameter("JSON",
				"{\"foobar\":\"this is a newly created node\"}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue("we got our newly created node",
				response.getContentAsString().matches(".*this is a newly created node.*"));
		// Now check the structure of our response, it should look something like:
		// {
		//  "numberOfItems":0,
		//  "foobar":"this is a newly created node",
		//  "items":[],
		//  "isFile":false,
		//  "created-at":"11/15/2010 19:58:54",
		//  "operations":{
		//     "directory":"/path/TestData/TestNode4/dir",
		//     "terms":"/path/TestData/TestNode4/annotations/terms",
		//     "annotations":"/path/TestData/TestNode4/annotations",
		//     "meta":"/path/TestData/TestNode4/meta"},
		//  "uri":"/path/TestData/TestNode4"
		// }
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(0, results.getInt("numberOfItems"));
		assertEquals(0, results.getJSONArray("items").length());
		assertEquals(false, results.getBoolean("isFile"));
		assertTrue(results.getString("created-at").matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
		assertEquals("this is a newly created node", results.getString("foobar"));
		assertEquals("/path/TestData/TestNode4/dir", results.getJSONObject("operations").getString("directory"));
		assertEquals("/path/TestData/TestNode4/annotations", results.getJSONObject("operations").getString("annotations"));
		assertEquals("/path/TestData/TestNode4/annotations/terms", results.getJSONObject("operations").getString("terms"));
		assertEquals("/path/TestData/TestNode4/meta", results.getJSONObject("operations").getString("meta"));
		assertEquals("/path/TestData/TestNode4", results.getString("uri"));
		
		/**
		 * Modify a node
		 */
		response = new MockHttpServletResponse();
		request.setParameter("JSON",
		        "{\"foobar\":\"this is a modified node\"}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue("we got our modified node",
				response.getContentAsString().matches(".*this is a modified node.*"));
		// Now check the structure of our response, it should look something like:
		// {
		//  "numberOfItems":0,
		//  "foobar":"this is a modified node",
		//  "items":[],
		//  "isFile":false,
		//  "created-at":"11/15/2010 19:58:54",
		//  "last-modified-at":"11/15/2010 19:59:24",
		//  "operations":{
		//     "directory":"/path/TestData/TestNode4/dir",
		//     "terms":"/path/TestData/TestNode4/annotations/terms",
		//     "annotations":"/path/TestData/TestNode4/annotations",
		//     "meta":"/path/TestData/TestNode4/meta"},
		//  "uri":"/path/TestData/TestNode4"
		// }
		results = new JSONObject(response.getContentAsString());
		assertEquals(0, results.getInt("numberOfItems"));
		assertEquals(0, results.getJSONArray("items").length());
		assertEquals(false, results.getBoolean("isFile"));
		assertTrue(results.getString("created-at").matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
		assertTrue(results.getString("last-modified-at").matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
		assertEquals("this is a modified node", results.getString("foobar"));
		assertEquals("/path/TestData/TestNode4/dir", results.getJSONObject("operations").getString("directory"));
		assertEquals("/path/TestData/TestNode4/annotations", results.getJSONObject("operations").getString("annotations"));
		assertEquals("/path/TestData/TestNode4/annotations/terms", results.getJSONObject("operations").getString("terms"));
		assertEquals("/path/TestData/TestNode4/meta", results.getJSONObject("operations").getString("meta"));
		assertEquals("/path/TestData/TestNode4", results.getString("uri"));

		/**
		 * Create a child node
		 */
		response = new MockHttpServletResponse();
		request.setRequestURI("/path/TestData/TestNode4/ChildA");
		request.setParameter("JSON",
				"{\"foobar\":\"this is a newly created child node\"}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue("we got our child node",
				response.getContentAsString().matches(".*this is a newly created child node.*"));
		// Now check the structure of our response, it should look something like:
		// {
		//  "numberOfItems":0,
		//  "foobar":"this is a newly created child node",
		//  "items":[],
		//  "isFile":false,
		//  "created-at":"11/15/2010 19:58:54",
		//  "operations":{
		//     "directory":"/path/TestData/TestNode4/ChildA/dir",
		//     "terms":"/path/TestData/TestNode4/ChildA/annotations/terms",
		//     "annotations":"/path/TestData/TestNode4/ChildA/annotations",
		//     "meta":"/path/TestData/TestNode4/ChildA/meta"},
		//  "uri":"/path/TestData/TestNode4/ChildA"
		// }
		results = new JSONObject(response.getContentAsString());
		assertEquals(0, results.getInt("numberOfItems"));
		assertEquals(0, results.getJSONArray("items").length());
		assertEquals(false, results.getBoolean("isFile"));
		assertTrue(results.getString("created-at").matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}"));
		assertEquals("this is a newly created child node", results.getString("foobar"));
		assertEquals("/path/TestData/TestNode4/ChildA/dir", results.getJSONObject("operations").getString("directory"));
		assertEquals("/path/TestData/TestNode4/ChildA/annotations", results.getJSONObject("operations").getString("annotations"));
		assertEquals("/path/TestData/TestNode4/ChildA/annotations/terms", results.getJSONObject("operations").getString("terms"));
		assertEquals("/path/TestData/TestNode4/ChildA/meta", results.getJSONObject("operations").getString("meta"));
		assertEquals("/path/TestData/TestNode4/ChildA", results.getString("uri"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.services.repositories.mvc.NodeCrudController#delete_by_post(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	@Test
	public final void testDelete_by_post() throws Exception {
		request.setMethod("POST");
		request.setRequestURI("/path/TestData/TestNode3/delete");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());

		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals("/path/TestData/TestNode3/delete", results.getString("uri"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.services.repositories.mvc.NodeCrudController#delete_by_delete(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	@Test
	public final void testDelete_by_delete() throws Exception {
		request.setMethod("DELETE");
		request.setRequestURI("/path/TestData/TestNode3");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());

		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals("/path/TestData/TestNode3", results.getString("uri"));
	}
}

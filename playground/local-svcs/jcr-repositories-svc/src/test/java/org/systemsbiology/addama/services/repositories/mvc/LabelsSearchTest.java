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
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;
import org.systemsbiology.addama.JcrTestHelper;

/**
 * Unit test suite for label (tag-like) CRUD operations for nodes.  This is 
 * testing both LabelController and JcrSearchController.
 * 
 * Dev Note: we are just checking search functionality here.  For tests 
 * that also check the structure of search results, see the 
 * JcrSearchController tests.  Also note that labels wind up looking 
 * much like other generic node properties when stored in the JCR so these 
 * search tests are somewhat redundant.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"NodeCrudControllerTest-context.xml"})
public class LabelsSearchTest {

	private static final Logger log = Logger
	.getLogger(LabelsSearchTest.class.getName());

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
		helper.createOrUpdateNode(
				"/TestData/TestNode1",
		        "{ 'aSingleWord':'same', 'anInteger':0, 'aBoolean':true, 'freeText':'stuff to be indexed for a free text search'}");
		helper.createOrUpdateNode(
				"/TestData/TestNode2",
				"{ 'aSingleWord':'same', 'anInteger':5, 'aBoolean':false, 'freeText':'unit tests are handy'}");
		helper.createOrUpdateNode(
				"/TestData/TestNode3",
				"{ 'aSingleWord':'same', 'anInteger':-10, 'aBoolean':false, 'freeText':'unit tests are helpful'}");

		// Wire up the jcrTemplate in the manner the controller expects
		MockHttpServletRequest setUpRequest = helper.getMockHttpServletRequest();

		// Add some labels to our test nodes
		setUpRequest.setMethod("POST");
		setUpRequest.setRequestURI("/path/TestData/TestNode1/labels");
		setUpRequest.setParameter("labels", "{\"labels\":[\"unit\"]}");
		servlet.service(setUpRequest, new MockHttpServletResponse());
		setUpRequest.setRequestURI("/path/TestData/TestNode2/labels");
		setUpRequest.setParameter("labels", "{\"labels\":[\"unique\", \"andFun\"]}");
		servlet.service(setUpRequest, new MockHttpServletResponse());
		setUpRequest.setRequestURI("/path/TestData/TestNode3/labels");
		setUpRequest.setParameter("labels", "{\"labels\":[\"andFun\"]}");
		servlet.service(setUpRequest, new MockHttpServletResponse());

		// Save these labels so that they are propagated into the search index
		helper.getJcrTemplate().save();
		
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
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testFreeTextSearch() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/search");
		
		// Search for something we expect to find in both labels and other fields
		request.setParameter("q", "unit");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(3, results.getInt("numberOfResults"));
	}
	
	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testFreeTextSearchLabelResultsOnly() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/search");
		
		// Search for something we only expect to find in labels
		request.setParameter("q", "unique");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(1, results.getInt("numberOfResults"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testLabelFieldSearch() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/search");
		
		// Search for a particular label value
		request.setParameter("labels", "andFun");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(2, results.getInt("numberOfResults"));
	}
	
	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testLabelFieldSearchLabelAlsoFoundInFreeText() throws Exception {
		request.setMethod("GET");
		request.setRequestURI("/search");
		
		// Search for a label whose value also resides in some node free text
		request.setParameter("labels", "unit");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(1, results.getInt("numberOfResults"));
	}
}

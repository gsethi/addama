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
 * Unit test suite for node 'label' property CRUD operations.  Labels can 
 * be used to tag nodes.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"NodeCrudControllerTest-context.xml"})
public class LabelsControllerTest {

	private static final Logger log = Logger
	.getLogger(LabelsControllerTest.class.getName());

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
		helper.createOrUpdateNode("/TestData/TestNode",
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
	 * Test method for
	 * {@link org.systemsbiology.addama.services.repositories.mvc.LabelsController#post(javax.servlet.http.HttpServletRequest, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testPost() throws Exception {
		// Create labels for a node
		request.setMethod("POST");
		request.setRequestURI("/path/TestData/TestNode/labels");
		request.setParameter("labels",
				"{\"labels\":[\"projectTag\", \"userTag\", \"typeTag\"]}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue(response.getContentAsString().matches(".*userTag.*"));
		// Now check the structure of our response, it should look something like:
		// {"labels":["typeTag","projectTag","userTag"],"uri":"/path/TestData/TestNode"}
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(3, results.getJSONArray("labels").length());
		assertEquals("/path/TestData/TestNode", results.getString("uri"));

		// Add a few more labels
		response = new MockHttpServletResponse();
		request.setParameter("labels","{\"labels\":[\"addMe\"]}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue(response.getContentAsString().matches(".*addMe.*"));
		// Now check the structure of our response, it should look something like:
		// {"labels":["typeTag","projectTag","userTag","addMe"],"uri":"/path/TestData/TestNode"}
		results = new JSONObject(response.getContentAsString());
		assertEquals(4, results.getJSONArray("labels").length());
		assertEquals("/path/TestData/TestNode", results.getString("uri"));
		
		// Overwrite our existing labels with new labels
		response = new MockHttpServletResponse();
		request.setParameter("labels",
		"{\"labels\":[\"some\", \"theOther\", \"blah\"]}");
		request.setParameter("overwrite", "true");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue(response.getContentAsString().matches(".*blah.*"));
		// Now check the structure of our response, it should look something like:
		// {"labels":["some","theOther","blah"],"uri":"/path/TestData/TestNode"}
		results = new JSONObject(response.getContentAsString());
		assertEquals(3, results.getJSONArray("labels").length());
		assertEquals("/path/TestData/TestNode", results.getString("uri"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.services.repositories.mvc.LabelsController#get(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testGet() throws Exception {
		// Bootstrap our test with some test data
		testPost();
		
		// Get the labels for a node
		request.setMethod("GET");
		request.setRequestURI("/path/TestData/TestNode/labels");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		assertTrue(response.getContentAsString().matches(".*blah.*"));
		// Now check the structure of our response, it should look something like:
		// {"labels":["some","theOther","blah"],"uri":"/path/TestData/TestNode"}
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(3, results.getJSONArray("labels").length());
		assertEquals("/path/TestData/TestNode", results.getString("uri"));
	}
}

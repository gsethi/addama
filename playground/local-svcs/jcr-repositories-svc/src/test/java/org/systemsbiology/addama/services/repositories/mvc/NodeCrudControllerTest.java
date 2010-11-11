/**
 *
 */
package org.systemsbiology.addama.services.repositories.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

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

		assertEquals(200, response.getStatus());
		assertTrue(response.getContentAsString().matches(
				".*unit tests are helpful.*"));
		assertTrue(response.getContentAsString().matches(
				"\\{\"aBoolean\":false,"
				+ "\"numberOfItems\":0,"
				+ "\"items\":\\[\\],"
				+ "\"isFile\":false,"
				+ "\"created-at\":\"\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\","
				+ "\"anInteger\":-10,"
				+ "\"freeText\":\"unit tests are helpful\","
				+ "\"aSingleWord\":\"same\","
				+ "\"operations\":\\{\"directory\":\"/path/TestData/TestNode3/dir\","
				+ "\"terms\":\"/path/TestData/TestNode3/annotations/terms\","
				+ "\"annotations\":\"/path/TestData/TestNode3/annotations\","
				+ "\"meta\":\"/path/TestData/TestNode3/meta\"\\},"
				+ "\"uri\":\"/path/TestData/TestNode3\"\\}"));
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
		request.setMethod("POST");
		request.setRequestURI("/path/TestData/TestNode4");
		request.setParameter("JSON",
				"{\"foobar\":\"this is a newly created node\"}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(200, response.getStatus());
		assertTrue(response.getContentAsString().matches(
				".*this is a newly created node.*"));
		assertTrue(response.getContentAsString().matches(
				"\\{\"numberOfItems\":0,"
				+ "\"foobar\":\"this is a newly created node\","
				+ "\"items\":\\[\\],"
				+ "\"isFile\":false,"
				+ "\"created-at\":\"\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\","
				+ "\"operations\":\\{\"directory\":\"/path/TestData/TestNode4/dir\","
				+ "\"terms\":\"/path/TestData/TestNode4/annotations/terms\","
				+ "\"annotations\":\"/path/TestData/TestNode4/annotations\","
				+ "\"meta\":\"/path/TestData/TestNode4/meta\"\\},"
				+ "\"uri\":\"/path/TestData/TestNode4\"\\}"));

		response = new MockHttpServletResponse();
		request.setParameter("JSON",
		        "{\"foobar\":\"this is a modified node\"}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(200, response.getStatus());
		assertTrue(response.getContentAsString().matches(
				".*this is a modified node.*"));
		assertTrue(response.getContentAsString().matches(
				"\\{\"numberOfItems\":0,"
				+ "\"foobar\":\"this is a modified node\","
				+ "\"items\":\\[\\],"
				+ "\"isFile\":false,"
				+ "\"created-at\":\"\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\","
				+ "\"operations\":\\{\"directory\":\"/path/TestData/TestNode4/dir\","
				+ "\"terms\":\"/path/TestData/TestNode4/annotations/terms\","
				+ "\"annotations\":\"/path/TestData/TestNode4/annotations\","
				+ "\"meta\":\"/path/TestData/TestNode4/meta\"\\},"
				+ "\"uri\":\"/path/TestData/TestNode4\","
				+ "\"last-modified-at\":\"\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\"\\}"));

		response = new MockHttpServletResponse();
		request.setRequestURI("/path/TestData/TestNode4/ChildA");
		request.setParameter("JSON",
				"{\"foobar\":\"this is a newly created child node\"}");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(200, response.getStatus());
		assertTrue(response.getContentAsString().matches(
				".*this is a newly created child node.*"));
		assertTrue(response.getContentAsString().matches(
				"\\{\"numberOfItems\":0,"
				+ "\"foobar\":\"this is a newly created child node\","
				+ "\"items\":\\[\\],"
				+ "\"isFile\":false,"
				+ "\"created-at\":\"\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\","
				+ "\"operations\":\\{\"directory\":\"/path/TestData/TestNode4/ChildA/dir\","
				+ "\"terms\":\"/path/TestData/TestNode4/ChildA/annotations/terms\","
				+ "\"annotations\":\"/path/TestData/TestNode4/ChildA/annotations\","
				+ "\"meta\":\"/path/TestData/TestNode4/ChildA/meta\"\\},"
				+ "\"uri\":\"/path/TestData/TestNode4/ChildA\"\\}"));
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

		assertEquals(200, response.getStatus());
		assertEquals("{\"uri\":\"/path/TestData/TestNode3/delete\"}", 
				response.getContentAsString());
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

		assertEquals(200, response.getStatus());
		assertEquals("{\"uri\":\"/path/TestData/TestNode3\"}", 
				response.getContentAsString());
	}
}

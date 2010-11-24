/**
 * 
 */
package org.systemsbiology.addama.services.repositories.mvc;

import static org.junit.Assert.*;

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
 * Unit test suite for node directory controllers which allow you to retrieve 
 * data about files and directories (node branches) underneath this node.
 * 
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 * 
 * @author deflaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"NodeCrudControllerTest-context.xml"})
public class DirectoryControllerTest {
		private static final Logger log = Logger
		.getLogger(DirectoryControllerTest.class.getName());

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
		 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.DirectoryController#directory(javax.servlet.http.HttpServletRequest)}.
		 *
		 * @throws Exception 
		 */
		@Test
		public final void testDirectoryParent() throws Exception {
			// Get a dir info for a node that is a parent
			request.setMethod("GET");
			request.setRequestURI("/path/TestData/TestParentNode/dir");
			response = new MockHttpServletResponse();
			servlet.service(request, response);
			log.info("Results: " + response.getContentAsString());
			assertEquals("we got 200 OK", 200, response.getStatus());
			// Now check the structure of our response, it should look something like:
			// {
			//  "numberOfFiles":0,
			//  "files":[],
			//  "directories":[{"isFile":false,"name":"TestChildNode","path":"/TestData/TestParentNode/TestChildNode","uri":"/path/TestData/TestParentNode/TestChildNode"}],
			//  "parent":"/path//TestData","
			//  "numberOfDirectories":1
			// }
			JSONObject results = new JSONObject(response.getContentAsString());
			assertEquals(0, results.getInt("numberOfFiles"));
			assertEquals(1, results.getInt("numberOfDirectories"));
			// TODO looks like there is a bug in the formulations of the parent value
		}

		/**
		 * Test method for {@link org.systemsbiology.addama.services.repositories.mvc.DirectoryController#directory(javax.servlet.http.HttpServletRequest)}.
		 *
		 * @throws Exception 
		 */
		@Test
		public final void testDirectoryChild() throws Exception {
			// Get dir info for a node that is a child
			request.setMethod("GET");
			request.setRequestURI("/path/TestData/TestParentNode/TestChildNode/dir");
			response = new MockHttpServletResponse();
			servlet.service(request, response);
			log.info("Results: " + response.getContentAsString());
			assertEquals("we got 200 OK", 200, response.getStatus());
			// Now check the structure of our response, it should look something like:
			// {
			//  "numberOfFiles":0,
			//  "files":[],
			//  "directories":[],
			//  "parent":"/path//TestData/TestParentNode",
			//  "numberOfDirectories":0
			// }
			JSONObject results = new JSONObject(response.getContentAsString());
			assertEquals(0, results.getInt("numberOfFiles"));
			assertEquals(0, results.getInt("numberOfDirectories"));
			// TODO looks like there is a bug in the formulations of the parent value
		}

}

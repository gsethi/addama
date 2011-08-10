package org.systemsbiology.addama.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.JcrTestHelper;

/**
 * Test suite for JCR search functionality regarding a search filter for recently
 * updated nodes
 * 
 * See
 * src/test/resources/org/systemsbiology/addama/rest/JcrSearchControllerTest-
 * context.xml for spring configuration specific to this test.
 * 
 * See src/test/resources/jackrabbit-testrepo.xml for spring configuration for
 * an in-memory JCR that can be used by any test suite
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "JcrSearchControllerTest-context.xml" })
public class RecentlyModifiedJcrSearchControllerTest {

	private static final Logger log = Logger
			.getLogger(RecentlyModifiedJcrSearchControllerTest.class.getName());

	private JcrSearchController searcher = new JcrSearchController();
	private MockHttpServletRequest request = null;
	private DateTime olderThanAllNodes = null;
	private DateTime inbetweenAllNodes = null;
	private DateTime newerThanAllNodes = null;

	@Autowired
	private JcrTestHelper helper = null;

	/**
	 * Note that it takes some sleep time to construct the data needed for each
	 * test. I tried to avoid that by using session.exportDocumentView() and
	 * session.importXML() but that winds up by-passing some addama-specific
	 * node-creation logic yielding incorrect test data. As an optimization
	 * TODO, consider constructing test data once for all tests in the class and
	 * reuse it the tests don't make any changes to the test state until the
	 * final test.
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		// Ensure that the JCR session remains open for the duration
		helper.obtainSession();

		// Add some test data to the JCR
		olderThanAllNodes = new DateTime();
		Thread.sleep(2000);
		helper
				.createOrUpdateNode(
						"/TestData/OlderNode1",
						"{ 'aSingleWord':'same', 'anInteger':0, 'aBoolean':true, 'freeText':'stuff to be indexed for a free text search'}");
		helper
				.createOrUpdateNode(
						"/TestData/OlderNode2",
						"{ 'aSingleWord':'same', 'anInteger':5, 'aBoolean':false, 'freeText':'unit tests are handy'}");
		helper
				.createOrUpdateNode(
						"/TestData/OlderNode3",
						"{ 'aSingleWord':'same', 'anInteger':-10, 'aBoolean':false, 'freeText':'unit tests are helpful'}");
		Thread.sleep(2000);
		inbetweenAllNodes = new DateTime();
		Thread.sleep(2000);
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
		Thread.sleep(2000);
		newerThanAllNodes = new DateTime();

		// Wire up the jcrTemplate in the manner the controller expects
		request = helper.getMockHttpServletRequest();
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
	public void testUpdatedMinOlderThanAllNodes() throws Exception {

		request.setParameter("aSingleWord", "same");
		request.setParameter("updated-min", olderThanAllNodes.toString());
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertTrue(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode3.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdatedMinOlderThanSomeNodes() throws Exception {

		request.setParameter("aSingleWord", "same");
		request.setParameter("updated-min", inbetweenAllNodes.toString());
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertTrue(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdatedMinOlderThanNoNodes() throws Exception {

		request.setParameter("aSingleWord", "same");
		request.setParameter("updated-min", newerThanAllNodes.toString());
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode3.*"));	
		
		// Now update a node and make sure it now shows up in search results
		helper.createOrUpdateNode("/TestData/OlderNode2", "{'foo':'bar'}");
		mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdatedMinOlderThanAllNodesFreeTextSearch()
			throws Exception {

		request.setParameter("q", "unit tests");
		request.setParameter("updated-min", olderThanAllNodes.toString());
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdatedMinOlderThanSomeNodesFreeTextSearch()
			throws Exception {

		request.setParameter("q", "unit tests");
		request.setParameter("updated-min", inbetweenAllNodes.toString());
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdatedMinOlderThanNoNodesFreeTextSearch() throws Exception {

		request.setParameter("q", "unit tests");
		request.setParameter("updated-min", newerThanAllNodes.toString());
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode3.*"));
		
		// Now update a node and make sure it now shows up in search results
		helper.createOrUpdateNode("/TestData/OlderNode2", "{'foo':'bar'}");
		mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*OlderNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*OlderNode3.*"));
	}

}

package org.systemsbiology.addama.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

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
 * Test suite for JCR search functionality
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
@ContextConfiguration
public class JcrSearchControllerTest {

	private static final Logger log = Logger
			.getLogger(JcrSearchControllerTest.class.getName());

	JcrSearchController searcher = new JcrSearchController();
	MockHttpServletRequest request = null;

	@Autowired
	private JcrTestHelper helper = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// Ensure that the JCR session remains open for the duration
		helper.obtainSession();

		// Add some test data to the JCR, note that all properties are indexed,
		// but currently JcrSearchController only allows search over string
		// values

		helper
				.createOrUpdateNode(
						"/TestNode1",
						"{ 'aSingleWord':'fun', 'anInteger':0, 'aBoolean':true, 'freeText':'stuff to be indexed for a free text search'}");
		helper
				.createOrUpdateNode(
						"/TestNode2",
						"{ 'aSingleWord':'foobar', 'anInteger':5, 'aBoolean':false, 'freeText':'unit tests are handy'}");
		helper
				.createOrUpdateNode(
						"/TestNode3",
						"{ 'aSingleWord':'hello', 'anInteger':-10, 'aBoolean':false, 'freeText':'unit tests are helpful'}");

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
	public void testFreeTextSearchNoResults() throws Exception {
		request.setParameter("q", "you cant find me");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertEquals("{json={}}", mav.getModel().toString());
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFreeTextSearchSingleResult() throws Exception {
		request.setParameter("q", "handy");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		// Looks like '"numberOfResults": 2' is no longer part of the results in
		// this version of the code, so we have to test each node that should
		// not be in the result set for
		// non-existence instead of using the count
		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFreeTextSearchMultipleResults() throws Exception {
		request.setParameter("q", "unit tests");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTextPropertySearch() throws Exception {
		request.setParameter("aSingleWord", "fun");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());
		
		assertTrue(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
	}
	
	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIntegerFreeTextSearch() throws Exception {
		request.setParameter("q", "5");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		// We correctly do not find any matching nodes because the number 5 does
		// not reside in any of the textual values
		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIntegerPropertySearch() throws Exception {
		request.setParameter("anInteger", "5");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());
		
		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertTrue(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBooleanFreeTextSearch() throws Exception {
		request.setParameter("q", "true");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		// We correctly do not find any matching nodes because the boolean true does
		// not reside in any of the textual values
		assertFalse(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
	}
	
	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBooleanPropertySearch() throws Exception {
		request.setParameter("aBoolean", "true");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());

		assertTrue(mav.getModel().toString().matches(".*TestNode1.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode2.*"));
		assertFalse(mav.getModel().toString().matches(".*TestNode3.*"));
	}
}

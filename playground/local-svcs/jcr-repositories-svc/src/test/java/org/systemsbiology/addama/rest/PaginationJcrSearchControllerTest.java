package org.systemsbiology.addama.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Set;
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
 * Test suite for JCR search functionality regarding pagination of search
 * results
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
public class PaginationJcrSearchControllerTest {

	private static final Logger log = Logger
			.getLogger(PaginationJcrSearchControllerTest.class.getName());
	private static final int NUM_TEST_NODES = 100;
	private static final String NODE_NAME_PREFIX = "\"name\":\"TestNode";

	private JcrSearchController searcher = new JcrSearchController();
	private MockHttpServletRequest request = null;

	@Autowired
	private JcrTestHelper helper = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		// Ensure that the JCR session remains open for the duration
		helper.obtainSession();

		// Toss a bunch of nodes into our JCR with a common property
		// so that a search can return all of them
		for (int i = 0; i < NUM_TEST_NODES; i++) {
			helper.createOrUpdateNode("/TestData/TestNode" + i,
					"{ 'aSingleWord':'same', 'anInteger':" + i + "}");
		}
		// Wire up the jcrTemplate in the manner the controller expects
		request = helper.getMockHttpServletRequest();
		
		// Add that common property to our search query
		request.setParameter("aSingleWord", "same");
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
	public void testSubsequentPages() throws Exception {
		int pageSize = 10;

		request.setParameter(JcrSearchController.MAX_RESULTS_PARAM, Integer
				.toString(pageSize));

		Set<String> nodesReturned = new HashSet<String>();
		for (int pageNum = 0; pageNum < (NUM_TEST_NODES / pageSize); pageNum++) {
			request.setParameter(JcrSearchController.START_INDEX_PARAM, Integer
					.toString(1 + (pageNum * pageSize)));
			ModelAndView mav = searcher.search(request);
			log.info("Results: " + mav.getModel());

			// Note that the current search implementation does not return the
			// results in any particular order. All we need to check here is
			// that we get the number of results back that we expect and a page
			// of results does not overlap a different page of results when no data
			// is changing in the JCR. Also note that I could have deserialized the
			// JSON instead of string matching for result parsing but I have a
			// feeling that the structure of the result object will change but the particular
			// string details this code matches on should remain the same.
			String matches[] = mav.getModel().toString().split(",");
			for (String match : matches) {
				if (match.startsWith(NODE_NAME_PREFIX)) {
					assertFalse(nodesReturned.contains(match));
					nodesReturned.add(match);
				}
			}
			assertEquals((1 + pageNum) * pageSize, nodesReturned.size());
		}

		// Make sure our test exercised all the results we expected
		assertEquals(NUM_TEST_NODES, nodesReturned.size());
	}
	
	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmptyPage() throws Exception {
		request.setParameter(JcrSearchController.MAX_RESULTS_PARAM, "10");
		request.setParameter(JcrSearchController.START_INDEX_PARAM, "101");
		ModelAndView mav = searcher.search(request);
		log.info("Results: " + mav.getModel());
		assertFalse(mav.getModel().toString().contains(NODE_NAME_PREFIX));
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBadStartIndex() throws Exception {
		ModelAndView mav = searcher.search(request);
		String expectedResults = mav.getModel().toString();

		// Invalid values for start index will be ignored and the default value
		// for that parameter will be used instead

		request.setParameter(JcrSearchController.START_INDEX_PARAM, Integer
				.toString(0));
		mav = searcher.search(request);
		assertEquals(expectedResults, mav.getModel().toString());

		request.setParameter(JcrSearchController.START_INDEX_PARAM, Integer
				.toString(-1));
		mav = searcher.search(request);
		assertEquals(expectedResults, mav.getModel().toString());

		request.setParameter(JcrSearchController.START_INDEX_PARAM, Integer
				.toString(-13));
		mav = searcher.search(request);
		assertEquals(expectedResults, mav.getModel().toString());
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.JcrSearchController#search(javax.servlet.http.HttpServletRequest)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBadMaxResults() throws Exception {
		ModelAndView mav = searcher.search(request);
		String expectedResults = mav.getModel().toString();

		// Invalid values for max results will be ignored and the default value
		// for that parameter will be used instead

		request.setParameter(JcrSearchController.MAX_RESULTS_PARAM, Integer
				.toString(0));
		mav = searcher.search(request);
		assertEquals(expectedResults, mav.getModel().toString());

		request.setParameter(JcrSearchController.MAX_RESULTS_PARAM, Integer
				.toString(-1));
		mav = searcher.search(request);
		assertEquals(expectedResults, mav.getModel().toString());

		request.setParameter(JcrSearchController.MAX_RESULTS_PARAM, Integer
				.toString(-13));
		mav = searcher.search(request);
		assertEquals(expectedResults, mav.getModel().toString());
	}
}

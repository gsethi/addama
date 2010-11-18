/**
 * 
 */
package org.systemsbiology.addama.gaesvcs.feeds.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * @author deflaux
 *
 */
public class FeedsControllerTest {
	private static final Logger log = Logger
	.getLogger(FeedsControllerTest.class.getName());
	   
	private final LocalServiceTestHelper memcacheHelper =
		new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig());
	private final LocalServiceTestHelper datastoreHelper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
	private MockHttpServletRequest request = null;
	private MockHttpServletResponse response = null;
    private FeedsController feedsController = new FeedsController();

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
		memcacheHelper.setUp();
		datastoreHelper.setUp();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		datastoreHelper.tearDown();
		// Dev Note: it appears that this second tearDown call is unnecessary, the prior
		// tearDown cleans up the resources and then this one gets a null pointer exception
		// memcacheHelper.tearDown(); 
	}

	/**
	 * Test method for {@link org.systemsbiology.addama.gaesvcs.feeds.mvc.FeedsController#handleRequestInternal
	 *                         (javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
	 * @throws Exception 
	 */
	@Test
	public void testHandleRequestInternal() throws Exception {
		request.setMethod("POST");
		request.setRequestURI("/feeds/happyCaseTest");
		request.setParameter("item",
				"{\"text\" : \"an RSS feed item\", \"date\" : \"Mon Nov 15 22:51:10 UTC 2009\", \"author\" : \"Unit Test\"}");
		ModelAndView mav = feedsController.handleRequestInternal(request, response); 
		log.info("Results: " + mav.getModel());
		JSONObject results = new JSONObject(mav.getModel().toString()).getJSONObject("json");
		// The response should be: {json={"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2009"}}
		assertEquals("an RSS feed item", results.getString("text"));
		
		request.setParameter("item",
		"{\"text\" : \"another RSS feed item\", \"date\" : \"Mon Nov 15 22:55:00 UTC 2010\", \"author\" : \"Unit Test\"}");
		mav = feedsController.handleRequestInternal(request, response); 
		log.info("Results: " + mav.getModel());
		results = new JSONObject(mav.getModel().toString()).getJSONObject("json");
		// The response should be: {json={"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"}}
		assertEquals("another RSS feed item", results.getString("text"));

		request.setMethod("GET");
		request.setRequestURI("/feeds/happyCaseTest");
		mav = feedsController.handleRequestInternal(request, response); 
		log.info("Results: " + mav.getModel());
		results = new JSONObject(mav.getModel().toString()).getJSONObject("json");
		// The response should be:
		//  {json={
		//  "items":[
		//      {"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"},
		//      {"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2009"}],
		//  "rss":"/feeds/happyCaseTest/rss",
		//  "uri":"/feeds/happyCaseTest"}}
		assertEquals(2, results.getJSONArray("items").length());
		assertEquals("/feeds/happyCaseTest/rss", results.getString("rss"));
	}

}

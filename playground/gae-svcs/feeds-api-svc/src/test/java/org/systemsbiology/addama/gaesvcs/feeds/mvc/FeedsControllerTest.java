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
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * The unit test suite for Google app engine service feeds-api-svcs uses local
 * instances of both DataStoreService and MemcacheService.
 *
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 *
 * Unfortunately I was not able to configure the DispatcherServlet with the
 * production configuration in files
 * src/main/webapp/WEB-INF/feeds-servlet.xml and
 * src/main/webapp/WEB-INF/app-contexts/controllers.xml because I was not able
 * to get them loaded correctly (probably just doing something silly).
 *
 * Instead see src/test/resources/test-feeds-servlet.xml for servlet context and
 * src/test/resources/org/systemsbiology/addama/gaesvcs/feeds/mvc/FeedsControllerTest-context.xml
 * for text context.
 *
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FeedsControllerTest {

    private static final Logger log = Logger
    .getLogger(FeedsControllerTest.class.getName());

    private final LocalServiceTestHelper memcacheHelper =
        new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig());
    private final LocalServiceTestHelper datastoreHelper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    private DispatcherServlet servlet = null;
    private MockHttpServletRequest request = null;
    private MockHttpServletResponse response = null;

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

        // Create a Spring MVC DispatcherServlet so that we can test our URL
        // mapping, request format, response format, and response status code.
        MockServletConfig servletConfig = new MockServletConfig("feeds");
        servletConfig.addInitParameter("contextConfigLocation", "/test-feeds-servlet.xml");
        // TODO load the production configuration directly, this doesn't work, I must be doing something silly
        // servletConfig.addInitParameter("contextConfigLocation",
        //         "classpath*:/WEB-INF/feeds-servlet.xml,classpath*:/WEB-INF/app-contexts/controllers.xml");
        servlet = new DispatcherServlet();
        servlet.init(servletConfig);

        request = new MockHttpServletRequest();
        request.addHeader("x-addama-registry-user", "unittest@addama.com");
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
    public void testAddItems() throws Exception {
        request.setMethod("POST");
        request.setRequestURI("/addama/feeds/happyCaseTest");
        request.setParameter("item",
                             "{\"text\" : \"an RSS feed item\", \"date\" : \"Mon Nov 15 22:51:10 UTC 2009\", \"author\" : \"Unit Test\"}");
        response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals("we got 200 OK", 200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2009"}
        assertEquals("an RSS feed item", results.getString("text"));


        request.setParameter("item",
                             "{\"text\" : \"another RSS feed item\", \"date\" : \"Mon Nov 15 22:55:00 UTC 2010\", \"author\" : \"Unit Test\"}");
        response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals("we got 200 OK", 200, response.getStatus());
        results = new JSONObject(response.getContentAsString());
        // The response should be: {"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"}
        assertEquals("another RSS feed item", results.getString("text"));
    }

    /**
     * Test method for {@link org.systemsbiology.addama.gaesvcs.feeds.mvc.FeedsController#handleRequestInternal
     *                         (javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     * @throws Exception
     */
    @Test
    public void testFeedList() throws Exception {
        // Bootstrap our test with a few other feed items
        testAddItems();

        request.setMethod("GET");
        request.setRequestURI("/addama/feeds");
        response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals("we got 200 OK", 200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be:
        // {"numberOfItems":1,"items":[{"uri":"/addama/feeds/happyCaseTest","creator":"unittest@addama.com"}]}
        assertEquals(1, results.getInt("numberOfItems"));
        assertTrue(results.getJSONArray("items").toString().matches(".*uri\":\"/addama/feeds/happyCaseTest\".*"));
        assertTrue(results.getJSONArray("items").toString().matches(".*creator\":\"unittest@addama.com.*"));
    }

    /**
     * Test method for {@link org.systemsbiology.addama.gaesvcs.feeds.mvc.FeedsController#handleRequestInternal
     *                         (javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     * @throws Exception
     */
    @Test
    public void testJsonFeed() throws Exception {
        // Bootstrap our test with a few other feed items
        testAddItems();

        request.setMethod("GET");
        request.setRequestURI("/addama/feeds/happyCaseTest");
        response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals("we got 200 OK", 200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be:
        //  {"numberOfItems":2,
        //   "items":[
        //      {"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2009"},
        //      {"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"}],
        //   "rss":"/addama/feeds/happyCaseTest/rss",
        //   "uri":"/addama/feeds/happyCaseTest"}
        assertEquals(2, results.getInt("numberOfItems"));
        assertTrue(results.getJSONArray("items").toString().matches(".*an RSS feed item.*"));
        assertTrue(results.getJSONArray("items").toString().matches(".*another RSS feed item.*"));
        assertEquals("/addama/feeds/happyCaseTest/rss", results.getString("rss"));
    }

    /**
     * Test method for {@link org.systemsbiology.addama.gaesvcs.feeds.mvc.FeedsController#handleRequestInternal
     *                         (javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     * @throws Exception
     */
    @Test
    public void testRssFeed() throws Exception {
        // Bootstrap our test with a few other feed items
        testAddItems();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.setRequestURI("/addama/feeds/happyCaseTest/rss");
        response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        // The response should be:
        // <?xml version="1.0" encoding="UTF-8"?>
        // <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:content="http://purl.org/rss/1.0/modules/content/">
        // <channel><title>Addama Feeds</title><link>/addama/feeds</link><description>Feeds for test.appspot.com</description>
        // <language>en</language><lastBuildDate>Thu, 18 Nov 2010 10:37:45 PST</lastBuildDate><generator>test.appspot.com</generator>
        // <ttl>60</ttl><image><url>/addama/services/feeds-api-svc/images/rss.png</url><title>Addama Feeds</title><link>/addama/feeds</link></image>
        // <item><title>an RSS feed item</title><link>/addama/feeds/happyCaseTest/rss</link><pubDate>Mon Nov 15 22:51:10 UTC 2009</pubDate>
        //    <description><![CDATA[an RSS feed item]]></description><content></content><author>Unit Test</author></item>
        // <item><title>another RSS feed item</title><link>/addama/feeds/happyCaseTest/rss</link><pubDate>Mon Nov 15 22:55:00 UTC 2010</pubDate>
        //    <description><![CDATA[another RSS feed item]]></description><content></content><author>Unit Test</author></item>
        // </channel></rss>
        assertEquals("we got 200 OK", 200, response.getStatus());
        assertTrue("looks like an RSS feed", response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss "));
        assertTrue("contains the item we expect", response.getContentAsString().matches(".*an RSS feed item.*"));
        assertTrue("contains the item we expect", response.getContentAsString().matches(".*another RSS feed item.*"));
        assertTrue("looks like an RSS feed", response.getContentAsString().endsWith("</channel></rss>"));
    }

}

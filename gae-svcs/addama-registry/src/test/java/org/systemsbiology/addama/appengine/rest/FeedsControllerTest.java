package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.DispatcherServlet;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;

import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.systemsbiology.addama.appengine.rest.FeedsController.PAGE_SIZE;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserEmail;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * The unit test suite for Google app engine service feeds-api-svcs uses local
 * instances of both DataStoreService and MemcacheService.
 * <p/>
 * The following unit tests are written at the servlet layer to test bugs in our
 * URL mapping, request format, response format, and response status code.
 * <p/>
 * Unfortunately I was not able to configure the DispatcherServlet with the
 * production configuration in files
 * src/main/webapp/WEB-INF/feeds-servlet.xml and
 * src/main/webapp/WEB-INF/app-contexts/controllers.xml because I was not able
 * to get them loaded correctly (probably just doing something silly).
 * <p/>
 * Instead see src/test/resources/test-feeds-servlet.xml for servlet context and
 * src/test/resources/org/systemsbiology/addama/gaesvcs/feeds/mvc/FeedsControllerTest-context.xml
 * for text context.
 *
 * @author deflaux
 */
@ContextConfiguration
public class FeedsControllerTest {

    private static final Logger log = Logger.getLogger(FeedsControllerTest.class.getName());

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig(), new LocalDatastoreServiceTestConfig());

    private DispatcherServlet servlet;

    @Before
    public void setUp() throws Exception {
        helper.setUp();

        Entity e = new Entity(createKey("api-keys", "mock-api-key"));
        e.setProperty("user", "unittest@addama.org");
        inTransaction(getDatastoreService(), new PutEntityTransactionCallback(e));

        // Create a Spring MVC DispatcherServlet so that we can test our URL
        // mapping, request format, response format, and response status code.
        MockServletConfig servletConfig = new MockServletConfig("feeds");
        servletConfig.addInitParameter("contextConfigLocation", "/test-feeds-servlet.xml");
        // TODO load the production configuration directly, this doesn't work, I must be doing something silly
        // servletConfig.addInitParameter("contextConfigLocation",
        //         "classpath*:/WEB-INF/feeds-servlet.xml,classpath*:/WEB-INF/app-contexts/controllers.xml");
        servlet = new DispatcherServlet();
        servlet.init(servletConfig);
    }

    @After
    public void tearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
        }
    }

    @Test
    public void testAddItems() throws Exception {
        MockHttpServletRequest request = newSignedRequest("POST", "/addama/feeds/happyTestCase");
        request.setParameter("item", "{\"text\" : \"an RSS feed item\", \"author\" : \"Unit Test\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2010"}
        assertEquals("an RSS feed item", results.getString("text"));

//        MockHttpServletRequest request = newSignedRequest("POST", "/addama/feeds/happyTestCase");
        request.setParameter("item", "{\"text\" : \"another RSS feed item\", \"author\" : \"Unit Test\"}");

        response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        results = new JSONObject(response.getContentAsString());
        // The response should be: {"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"}
        assertEquals("another RSS feed item", results.getString("text"));
    }

    @Test
    public void testFeedList() throws Exception {
        testAddItems();

        MockHttpServletRequest request = newSignedRequest("GET", "/addama/feeds");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());

        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be:
        // {"numberOfItems":1,"items":[{"uri":"/addama/feeds/happyTestCase","creator":"unittest@addama.org"}]}
        assertEquals(1, results.getInt("numberOfItems"));
        assertTrue(results.getJSONArray("items").toString().matches(".*uri\":\"/addama/feeds/happyTestCase\".*"));
        assertTrue(results.getJSONArray("items").toString().matches(".*creator\":\"unittest@addama.org.*"));
    }

    @Test
    public void testJsonFeed() throws Exception {
        testAddItems();

        MockHttpServletRequest request = newSignedRequest("GET", "/addama/feeds/happyTestCase");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be:
        //  {"numberOfItems":2,
        //   "items":[
        //      {"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2010"},
        //      {"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"}],
        //   "rss":"/addama/feeds/happyTestCase/rss?page=1",
        //   "uri":"/addama/feeds/happyTestCase?page=1"}
        assertEquals(2, results.getInt("numberOfItems"));
        assertTrue(results.getJSONArray("items").toString().matches(".*an RSS feed item.*"));
        assertTrue(results.getJSONArray("items").toString().matches(".*another RSS feed item.*"));
        assertEquals("/addama/feeds/happyTestCase/rss?page=1", results.getString("rss"));
    }

    @Test
    public void testRssFeed() throws Exception {
        testAddItems();

        MockHttpServletRequest request = newSignedRequest("GET", "/addama/feeds/happyTestCase/rss");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        // The response should be:
        // <?xml version="1.0" encoding="UTF-8"?>
        // <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:content="http://purl.org/rss/1.0/modules/content/" 
        //  xmlns:atom="http://www.w3.org/2005/Atom">
        // <channel><title>Addama Feeds</title><link>https://test.appspot.com/addama/feeds/happyTestCase?page=1</link>
        // <atom:link rel="next" href="https://test.appspot.com/addama/feeds/happyTestCase?page=2" />
        // <description>Feeds for test.appspot.com</description><language>en</language>
        // <lastBuildDate>Thu, 18 Nov 2010 10:37:45 PST</lastBuildDate><generator>test.appspot.com</generator>
        // <ttl>60</ttl><image><url>/images/rss.png</url><title>Addama Feeds</title><link>/addama/feeds</link></image>
        // <item><title>an RSS feed item</title><link>/addama/feeds/happyTestCase/rss</link><pubDate>Mon Nov 15 22:51:10 UTC 2010</pubDate>
        //    <description><![CDATA[an RSS feed item]]></description><content></content><author>Unit Test</author></item>
        // <item><title>another RSS feed item</title><link>/addama/feeds/happyTestCase/rss</link><pubDate>Mon Nov 15 22:55:00 UTC 2010</pubDate>
        //    <description><![CDATA[another RSS feed item]]></description><content></content><author>Unit Test</author></item>
        // </channel></rss>

        assertTrue("looks like an RSS feed",
                response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss "));
        assertTrue("contains the item we expect",
                response.getContentAsString().matches(".*an RSS feed item.*"));
        assertTrue("contains the item we expect",
                response.getContentAsString().matches(".*another RSS feed item.*"));
        assertTrue("looks like an RSS feed",
                response.getContentAsString().endsWith("</channel></rss>"));
    }

    @Test
    public void testAddItemsWithOptionalFields() throws Exception {
        // Bootstrap our test with a few other feed items
        testAddItems();

        MockHttpServletRequest request = newSignedRequest("POST", "/addama/feeds/happyTestCase");
        request.setParameter("item", "{\"title\":\"this is a title\", \"link\":\"http://thedailykitten.com/\","
                + "\"text\" : \"a third RSS feed item\", \"author\" : \"Unit Test\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        assertEquals(200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be: {"author":"Unit Test","text":"an RSS feed item","title":"this is a title",
        // "link":"http://thedailykitten.com/","date":"Mon Nov 15 22:51:10 UTC 2010"}
        assertEquals("a third RSS feed item", results.getString("text"));
        assertEquals("this is a title", results.getString("title"));
        assertEquals("http://thedailykitten.com/", results.getString("link"));
    }

    @Test
    public void testJsonFeedWithOptionalFields() throws Exception {
        // Bootstrap our test with a few other feed items
        testAddItemsWithOptionalFields();

        MockHttpServletRequest request = newSignedRequest("GET", "/addama/feeds/happyTestCase");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        // The response should be:
        //  {"numberOfItems":3,
        //   "items":[
        //      {"author":"Unit Test","text":"a third RSS feed item","title":"this is a title","link":"http://thedailykitten.com/","date":"Mon Nov 15 22:51:10 UTC 2010"},
        //      {"author":"Unit Test","text":"another RSS feed item","date":"Mon Nov 15 22:55:00 UTC 2010"},
        //      {"author":"Unit Test","text":"an RSS feed item","date":"Mon Nov 15 22:51:10 UTC 2010"}]
        //   "rss":"/addama/feeds/happyTestCase/rss?page=1",
        //   "uri":"/addama/feeds/happyTestCase?page=1"}
        assertEquals(3, results.getInt("numberOfItems"));
        assertTrue(results.getJSONArray("items").toString().matches(".*this is a title.*"));
    }

    @Test
    public void testRSSFeedWithOptionalFields() throws Exception {
        // Bootstrap our test with a few other feed items
        testAddItems();
        MockHttpServletRequest request1 = newSignedRequest("POST", "/addama/feeds/happyTestCase");
        request1.setParameter("item", "{\"title\":\"this is a title\", \"link\":\"http://thedailykitten.com/\","
                + "\"text\" : \"a third RSS feed item\", \"author\" : \"Unit Test\"}");
        servlet.service(request1, new MockHttpServletResponse());


        MockHttpServletRequest request = newSignedRequest("GET", "/addama/feeds/happyTestCase/rss");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        // The response should be:
        // <?xml version="1.0" encoding="UTF-8"?><rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" 
        // xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><title>Addama Feeds</title><link>/addama/feeds</link>
        // <description>Feeds for test.appspot.com</description><language>en</language><lastBuildDate>Thu, 18 Nov 2010 13:36:13 PST</lastBuildDate>
        // <generator>test.appspot.com</generator><ttl>60</ttl><image><url>/images/rss.png</url><title>Addama Feeds</title>
        // <link>/addama/feeds</link></image>
        // <item><title>this is a title</title><link>http://thedailykitten.com/</link><pubDate>Mon Nov 15 22:51:10 UTC 2010</pubDate>
        // <description><![CDATA[a third RSS feed item]]></description><content></content><author>Unit Test</author></item>
        // <item><title>another RSS feed item</title><link>/addama/feeds/happyTestCase/rss</link><pubDate>Mon Nov 15 22:55:00 UTC 2010</pubDate>
        // <description><![CDATA[another RSS feed item]]></description><content></content><author>Unit Test</author></item>
        // <item><title>an RSS feed item</title><link>/addama/feeds/happyTestCase/rss</link><pubDate>Mon Nov 15 22:51:10 UTC 2010</pubDate>
        // <description><![CDATA[an RSS feed item]]></description><content></content><author>Unit Test</author></item></channel></rss>
        assertTrue("looks like an RSS feed",
                response.getContentAsString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss "));
        assertTrue("contains the item we expect",
                response.getContentAsString().matches(".*an RSS feed item.*"));
        assertTrue("contains the item we expect",
                response.getContentAsString().matches(".*another RSS feed item.*"));
        assertTrue("contains the item we expect",
                response.getContentAsString().matches(".*this is a title.*"));
        assertTrue("looks like an RSS feed",
                response.getContentAsString().endsWith("</channel></rss>"));
    }

    @Test
    public void testIdempotentAdd() throws Exception {
        JSONObject item = new JSONObject("{\"text\" : \"the same\", \"author\" : \"Unit Test\"}");

        MockHttpServletRequest request = newSignedRequest("POST", "/addama/feeds/happyTestCase");
        request.setParameter("item", item.toString());

        // Add the exact same item three times, but two of the times use the same "key"
        // The end result should be two items in our feed instead of three.
        // Dev note: this isn't idempotent in the most strict sense because the new item overwrites the old item.
        // It is idempotent in a loose sense in that the feed does not grow longer.
        servlet.service(request, new MockHttpServletResponse());
        item.put("key", "there can only be one of these");
        request.setParameter("item", item.toString());
        servlet.service(request, new MockHttpServletResponse());
        servlet.service(request, new MockHttpServletResponse());

        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        assertEquals(2, results.getInt("numberOfItems"));
    }

    @Test
    public void testSortedByDate() throws Exception {
        MockHttpServletRequest request = newSignedRequest("POST", "/addama/feeds/happyTestCase");

        // Add items out of order by date descending, then confirm that they are sorted in the feed
        request.setParameter("item", "{\"text\":\"third\", \"date\":\"2008-11-01\"}");
        servlet.service(request, new MockHttpServletResponse());
        request.setParameter("item", "{\"text\":\"first\", \"date\":\"2010-11-01\"}");
        servlet.service(request, new MockHttpServletResponse());
        request.setParameter("item", "{\"text\":\"fourth\", \"date\":\"2007-11-01\"}");
        servlet.service(request, new MockHttpServletResponse());
        request.setParameter("item", "{\"text\":\"second\", \"date\":\"2009-11-01\"}");
        servlet.service(request, new MockHttpServletResponse());

        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        log.info("Results: " + response.getContentAsString());
        assertEquals(200, response.getStatus());
        JSONObject results = new JSONObject(response.getContentAsString());
        assertEquals(4, results.getInt("numberOfItems"));
        JSONArray items = results.getJSONArray("items");
        assertEquals("first", ((JSONObject) items.get(0)).getString("text"));
        assertEquals("second", ((JSONObject) items.get(1)).getString("text"));
        assertEquals("third", ((JSONObject) items.get(2)).getString("text"));
        assertEquals("fourth", ((JSONObject) items.get(3)).getString("text"));
    }

    @Test
    public void testPagination() throws Exception {
        final int numTestPages = 10;

        // Add some test data to a feed in order from 1 to 100
        int i;
        for (i = 1; i <= (numTestPages * PAGE_SIZE); i++) {
            addPaginationTestFeedItem(i);
        }

        // Check that we get items 100 through 1 in reverse order
        confirmCorrectPagination(numTestPages, (numTestPages * PAGE_SIZE));

        // Check that a page we expect to be empty is empty
        JSONObject results = getPaginationTestFeedPage(numTestPages + 1);
        assertEquals(0, results.getJSONArray("items").length());

        // Add a new item to the feed
        addPaginationTestFeedItem(i);

        // Check that we get items 101 through 2 in reverse order
        confirmCorrectPagination(numTestPages, (numTestPages * PAGE_SIZE) + 1);

        // Check that an item spilled over to a subsequent page
        results = getPaginationTestFeedPage(numTestPages + 1);
        assertEquals(1, results.getInt("numberOfItems"));

        // Check that a page we expect to be empty is empty
        results = getPaginationTestFeedPage(numTestPages + 2);
        assertEquals(0, results.getInt("numberOfItems"));
    }

    private void addPaginationTestFeedItem(int itemNum) throws Exception {
        MockHttpServletRequest request = newSignedRequest("POST", "/addama/feeds/happyTestCase");
        request.setParameter("item", "{\"text\":\"item number " + itemNum + "\"}");
        servlet.service(request, new MockHttpServletResponse());
    }

    private JSONObject getPaginationTestFeedPage(int page) throws Exception {
        MockHttpServletRequest request = newSignedRequest("GET", "/addama/feeds/happyTestCase");
        request.setParameter("page", "" + page);
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        return new JSONObject(response.getContentAsString());
    }

    private void confirmCorrectPagination(int numPagesToCheck, int mostRecentItem) throws Exception {
        // Loop over pages from 1 to numPagesToCheck ensuring that we get items in monotonically
        // decreasing order starting with mostRecentItem
        int expectedItemNum = mostRecentItem;
        for (int page = 1; page <= numPagesToCheck; page++) {
            JSONObject results = getPaginationTestFeedPage(page);
            assertEquals(PAGE_SIZE, results.getInt("numberOfItems"));
            JSONArray items = results.getJSONArray("items");
            for (int i = 0; i < PAGE_SIZE; i++) {
                assertEquals("item number " + expectedItemNum, items.getJSONObject(i).getString("text"));
                expectedItemNum--;
            }
        }
    }

    private MockHttpServletRequest newSignedRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-addama-apikey", "mock-api-key");
        request.setMethod(method);
        request.setRequestURI(uri);
        assertEquals("unittest@addama.org", getLoggedInUserEmail(request));
        return request;
    }

}
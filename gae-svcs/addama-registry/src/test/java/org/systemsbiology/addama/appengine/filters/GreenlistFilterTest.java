package org.systemsbiology.addama.appengine.filters;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;

import java.util.HashMap;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.appengine.util.Greenlist.BOUNCER;
import static org.systemsbiology.addama.appengine.util.Greenlist.isGreenlistActive;

/**
 * @author hrovira
 */
public class GreenlistFilterTest {
    private static final String GOOD_USER = "gooduser@addama.org";
    private static final String BAD_USER = "baduser@addama.org";

    private LocalServiceTestHelper helper;
    private GreenlistFilter FILTER;
    private MockHttpServletRequest REQUEST;
    private MockHttpServletResponse RESPONSE;
    private MockFilterChain FILTER_CHAIN;

    @Before
    public void setUp() throws Exception {
        HashMap<String, Object> envAttr = new HashMap<String, Object>();
        envAttr.put("com.google.appengine.api.users.UserService.user_id_key", "10");
        helper = new LocalServiceTestHelper(new LocalUserServiceTestConfig(), new LocalMemcacheServiceTestConfig(), new LocalDatastoreServiceTestConfig());
        helper.setEnvEmail(GOOD_USER);
        helper.setEnvIsLoggedIn(true);
        helper.setEnvAuthDomain("addama.org");
        helper.setEnvAttributes(envAttr);
        helper.setUp();

        FILTER = new GreenlistFilter();
        REQUEST = new MockHttpServletRequest();
        RESPONSE = new MockHttpServletResponse();
        FILTER_CHAIN = new MockFilterChain();
    }

    @After
    public void tearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
        }
    }

    @Test
    public void notLoggedIn() throws Exception {
        helper.setEnvIsLoggedIn(false).setUp();
        setupGreenlist();

        FILTER.doFilter(REQUEST, RESPONSE, FILTER_CHAIN);
        assertTrue(SC_UNAUTHORIZED == RESPONSE.getStatus());
    }

    @Test
    public void noGreenlist() throws Exception {
        helper.setEnvEmail(BAD_USER).setUp();
        assertFalse(isGreenlistActive());
        FILTER.doFilter(REQUEST, RESPONSE, FILTER_CHAIN);
        assertTrue(SC_UNAUTHORIZED != RESPONSE.getStatus());
    }

    @Test
    public void inGreenlist() throws Exception {
        helper.setEnvEmail(GOOD_USER).setUp();
        setupGreenlist();

        FILTER.doFilter(REQUEST, RESPONSE, FILTER_CHAIN);
        assertTrue(SC_UNAUTHORIZED != RESPONSE.getStatus());
    }

    @Test
    public void notInGreenlist() throws Exception {
        helper.setEnvEmail(BAD_USER).setUp();
        setupGreenlist();

        FILTER.doFilter(REQUEST, RESPONSE, FILTER_CHAIN);
        assertTrue(SC_UNAUTHORIZED == RESPONSE.getStatus());
    }

    /*
    * Private Methods
    */
    private void setupGreenlist() {
        inTransaction(getDatastoreService(), new PutEntityTransactionCallback(new Entity(createKey("greenlist", BOUNCER))));
        inTransaction(getDatastoreService(), new PutEntityTransactionCallback(new Entity(createKey("greenlist", GOOD_USER))));
        assertTrue(isGreenlistActive());
    }

}

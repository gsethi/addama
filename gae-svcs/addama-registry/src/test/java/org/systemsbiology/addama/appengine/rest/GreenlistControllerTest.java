package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import java.util.HashSet;

import static org.junit.Assert.*;
import static org.systemsbiology.addama.appengine.util.Greenlist.isGreenlistActive;
import static org.systemsbiology.addama.appengine.util.Greenlist.isGreenlisted;

/**
 * @author hrovira
 */
public class GreenlistControllerTest {
    private LocalServiceTestHelper helper;
    private GreenlistController CONTROLLER;

    @Before
    public void setUp() throws Exception {
        helper = new LocalServiceTestHelper(new LocalUserServiceTestConfig(),
                new LocalMemcacheServiceTestConfig(), new LocalDatastoreServiceTestConfig());
        helper.setEnvEmail("admin@addama.org");
        helper.setEnvIsLoggedIn(true);
        helper.setEnvAuthDomain("addama.org");
        helper.setUp();

        CONTROLLER = new GreenlistController();
    }

    @After
    public void tearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
        }
    }

    @Test
    public void noGreenlist() throws Exception {
        helper.setEnvIsAdmin(true);

        ModelAndView mav = CONTROLLER.list(new MockHttpServletRequest());
        assertNotNull(mav);

        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertFalse(json.has("numberOfItems"));
        assertFalse(json.has("items"));
    }

    @Test
    public void addUsers() throws Exception {
        helper.setEnvIsAdmin(true);

        for (int i = 0; i < 10; i++) {
            String user = i + "@addama.org";
            CONTROLLER.addUser(new MockHttpServletRequest("POST", "/addama/greenlist/" + user));
        }
        assertTrue(isGreenlistActive());

        for (int i = 0; i < 10; i++) {
            assertTrue(isGreenlisted(i + "@addama.org"));
        }
        assertFalse(isGreenlisted("33@addama.org"));

        ModelAndView mav = CONTROLLER.list(new MockHttpServletRequest());
        assertNotNull(mav);

        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertTrue(json.has("items"));

        JSONArray items = json.getJSONArray("items");
        assertEquals(10, items.length());

        HashSet<String> foundUsers = new HashSet<String>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            foundUsers.add(item.getString("id"));
        }
        assertEquals(10, foundUsers.size());
        for (int i = 0; i < 10; i++) {
            assertTrue(foundUsers.contains(i + "@addama.org"));
        }
    }

    @Test(expected = ForbiddenAccessException.class)
    public void notAdmin_list() throws Exception {
        helper.setEnvIsAdmin(false);
        CONTROLLER.list(new MockHttpServletRequest());
    }

    @Test(expected = ForbiddenAccessException.class)
    public void notAdmin_addUsers() throws Exception {
        helper.setEnvIsAdmin(false);
        CONTROLLER.list(new MockHttpServletRequest());
    }
}

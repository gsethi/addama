package org.systemsbiology.addama.appengine.rest.memberships;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;
import static org.junit.Assert.*;
import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;

/**
 * @author hrovira
 */
public class DomainMembershipControllerTest {
    private static final String JSON_ARRAY = "[{user:'hector@addama.org',membership:'member'}]";

    private final AnnotationMethodHandlerAdapter handlerAdapter = new AnnotationMethodHandlerAdapter();
    private final DomainMembershipController controller = new DomainMembershipController();
    private LocalServiceTestHelper helper;

    @Before
    public void setUp() {
        helper = new LocalServiceTestHelper(new LocalUserServiceTestConfig(), new LocalDatastoreServiceTestConfig());
        helper.setEnvIsAdmin(true);
        helper.setEnvIsLoggedIn(true);
        helper.setUp();

        getCurrentEnvironment().getAttributes().put("com.google.appengine.runtime.default_version_hostname", "testappspot");
    }

    @After
    public void tearDown() {
        if (helper != null) {
            helper.tearDown();
        }
    }

    @Test
    public void set_domain_users() throws Exception {
        MockHttpServletRequest POST = new MockHttpServletRequest();
        POST.setMethod("POST");
        POST.setRequestURI("/memberships/domain/users");
        POST.setParameter("users", JSON_ARRAY);

        ModelAndView post_mav = handlerAdapter.handle(POST, new MockHttpServletResponse(), controller);
        assertNotNull(post_mav);
        assertNotNull(post_mav.getView());
        assertEquals(OkResponseView.class, post_mav.getView().getClass());

        MockHttpServletRequest GET = new MockHttpServletRequest();
        GET.setMethod("GET");
        GET.setRequestURI("/memberships/domain/users");

        ModelAndView get_mav = handlerAdapter.handle(GET, new MockHttpServletResponse(), controller);
        JSONObject json = assertAndReturnModelAttributeOfType(get_mav, "json", JSONObject.class);
        assertNotNull(json);

        assertTrue(json.has("items"));
        JSONArray items = json.getJSONArray("items");
        assertNotNull(items);
        assertEquals(1, items.length());

        JSONObject item = items.getJSONObject(0);
        assertNotNull(item);
        assertTrue(item.has("user"));
        assertTrue(item.has("membership"));

        assertEquals("hector@addama.org", item.getString("user"));
        assertEquals("member", item.getString("membership"));
    }
}

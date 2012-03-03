package org.systemsbiology.addama.appengine.rest;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
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
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.systemsbiology.addama.appengine.util.JsonStore.*;

/**
 * @author hrovira
 */
public class JsonStoreControllerTest {
    private JsonStoreController CONTROLLER;
    private LocalServiceTestHelper helper;

    @Before
    public void setup() {
        helper = new LocalServiceTestHelper(new LocalUserServiceTestConfig(), new LocalDatastoreServiceTestConfig());
        helper.setEnvEmail("admin@addama.org");
        helper.setEnvIsLoggedIn(true);
        helper.setEnvAuthDomain("addama.org");
        helper.setUp();

        CONTROLLER = new JsonStoreController();
    }

    @After
    public void tearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
        }
    }

    @Test
    public void listStores() throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveStore("store_" + i, new JSONObject().put("label", "store " + i).put("identity", i));
        }

        ModelAndView mav = CONTROLLER.listStores(new MockHttpServletRequest());
        assertNotNull(mav);

        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertTrue(json.has("items"));
        JSONArray items = json.getJSONArray("items");
        assertNotNull(items);
        assertEquals(10, items.length());

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            assertNotNull(item);
            assertTrue(item.has("identity"));

            int identity = item.getInt("identity");
            assertEquals("store_" + identity, item.getString("id"));
            assertEquals("store " + identity, item.getString("label"));
        }
    }

    @Test
    public void listStore() throws Exception {
        HashMap<Integer, String> itemIds = new HashMap<Integer, String>();
        for (int i = 1; i <= 10; i++) {
            JSONObject item = new JSONObject();
            item.put("label", "item " + i);
            item.put("identity", i);
            item.put("isEvenNumber", (i % 2 == 0));
            UUID id = createItem("store_one", item);
            itemIds.put(i, id.toString());
        }

        ModelAndView mav = CONTROLLER.listStore(new MockHttpServletRequest(), "store_one");
        assertNotNull(mav);

        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertTrue(json.has("items"));
        JSONArray items = json.getJSONArray("items");
        assertNotNull(items);
        assertEquals(10, items.length());

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            assertNotNull(item);
            assertTrue(item.has("identity"));

            int identity = item.getInt("identity");
            boolean isEvenNumber = (identity % 2 == 0);

            assertTrue(itemIds.containsKey(identity));
            assertEquals(itemIds.get(identity), item.getString("id"));
            assertEquals("item " + identity, item.getString("label"));
            assertEquals(isEvenNumber, item.getBoolean("isEvenNumber"));
        }
    }

    @Test
    public void listItem() throws Exception {
        JSONObject item = new JSONObject();
        item.put("label", "item one");
        item.put("identity", 354);
        item.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        item.put("isMarkedByABooleanFlag", true);
        UUID id = createItem("store_one", item);

        ModelAndView mav = CONTROLLER.listItem(new MockHttpServletRequest(), "store_one", id.toString());
        assertNotNull(mav);

        JSONObject json = (JSONObject) mav.getModel().get("json");
        assertNotNull(json);
        assertTrue(json.has("id"));
        assertTrue(json.has("label"));
        assertTrue(json.has("identity"));
        assertTrue(json.has("description"));
        assertTrue(json.has("isMarkedByABooleanFlag"));

        assertEquals(id.toString(), json.getString("id"));
        assertEquals("item one", json.getString("label"));
        assertEquals(354, json.getInt("identity"));
        assertTrue(json.getBoolean("isMarkedByABooleanFlag"));
    }

    @Test
    public void creation() throws Exception {
        helper.setEnvIsAdmin(true);

        JSONObject item = new JSONObject();
        item.put("label", "item one");
        item.put("identity", 354);
        item.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        item.put("isMarkedByABooleanFlag", true);
        UUID id = createItem("store_one", item);

        CONTROLLER.creation(new MockHttpServletRequest(), "store_one", null, item);

        JSONObject actual = retrieveItem("store_one", id.toString());
        assertNotNull(actual);
        assertEquals(id.toString(), actual.getString("id"));
        assertEquals(item.getString("label"), actual.getString("label"));
        assertEquals(item.getInt("identity"), actual.getInt("identity"));
        assertEquals(item.getString("description"), actual.getString("description"));
        assertEquals(item.getBoolean("isMarkedByABooleanFlag"), actual.getBoolean("isMarkedByABooleanFlag"));
    }

    @Test
    public void creation_namedItem() throws Exception {
        helper.setEnvIsAdmin(true);

        JSONObject item = new JSONObject();
        item.put("label", "item one");
        item.put("identity", 354);
        item.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        item.put("isMarkedByABooleanFlag", true);

        CONTROLLER.update(new MockHttpServletRequest(), "store_one", "item_one", item);

        JSONObject actual = retrieveItem("store_one", "item_one");
        assertNotNull(actual);
        assertEquals("item_one", actual.getString("id"));
        assertEquals(item.getString("label"), actual.getString("label"));
        assertEquals(item.getInt("identity"), actual.getInt("identity"));
        assertEquals(item.getString("description"), actual.getString("description"));
        assertEquals(item.getBoolean("isMarkedByABooleanFlag"), actual.getBoolean("isMarkedByABooleanFlag"));
    }

    @Test
    public void creation_nestedJSON() throws Exception {
        helper.setEnvIsAdmin(true);

        JSONObject item = new JSONObject();
        item.put("label", "item one");
        item.put("identity", 354);
        item.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        item.put("isMarkedByABooleanFlag", true);
        item.put("subItem", new JSONObject().put("prop", "val"));

//        UUID id = createItem("store_one", item);

        ModelAndView mav = CONTROLLER.creation(new MockHttpServletRequest(), "store_one", null, item);
        JSONObject respJson = (JSONObject) mav.getModel().get("json");
        assertNotNull(respJson);
        assertTrue(respJson.has("id"));
        String id = respJson.getString("id");

        mav = CONTROLLER.listItem(new MockHttpServletRequest(), "store_one", id);
        JSONObject actual = (JSONObject) mav.getModel().get("json");
        assertNotNull(actual);
        assertEquals(id, actual.getString("id"));
        assertEquals(item.getString("label"), actual.getString("label"));
        assertEquals(item.getInt("identity"), actual.getInt("identity"));
        assertEquals(item.getString("description"), actual.getString("description"));
        assertEquals(item.getBoolean("isMarkedByABooleanFlag"), actual.getBoolean("isMarkedByABooleanFlag"));
        assertTrue(item.has("subItem"));

        JSONObject subItem = item.getJSONObject("subItem");
        assertNotNull(subItem);
        assertEquals("val", subItem.get("prop"));
    }

    @Test
    public void update() throws Exception {
        helper.setEnvIsAdmin(true);

        JSONObject item = new JSONObject();
        item.put("label", "item one");
        item.put("identity", 354);
        item.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        item.put("isMarkedByABooleanFlag", true);
        UUID id = createItem("store_one", item);

        CONTROLLER.creation(new MockHttpServletRequest(), "store_one", null, item);

        JSONObject updated = new JSONObject();
        updated.put("label", "item updated");
        updated.put("identity", 333);
        updated.put("isMarkedByABooleanFlag", false);

        CONTROLLER.update(new MockHttpServletRequest(), "store_one", id.toString(), updated);

        JSONObject actual = retrieveItem("store_one", id.toString());
        assertNotNull(actual);
        assertEquals(id.toString(), actual.getString("id"));

        assertNotSame(item.getString("label"), actual.getString("label"));
        assertNotSame(item.getInt("identity"), actual.getInt("identity"));
        assertNotSame(item.getBoolean("isMarkedByABooleanFlag"), actual.getBoolean("isMarkedByABooleanFlag"));

        assertEquals(item.getString("description"), actual.getString("description"));

        assertEquals(updated.getString("label"), actual.getString("label"));
        assertEquals(updated.getInt("identity"), actual.getInt("identity"));
        assertEquals(updated.getBoolean("isMarkedByABooleanFlag"), actual.getBoolean("isMarkedByABooleanFlag"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void delete() throws Exception {
        helper.setEnvIsAdmin(true);

        JSONObject item = new JSONObject();
        item.put("label", "item one");
        item.put("identity", 354);
        item.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        item.put("isMarkedByABooleanFlag", true);
        UUID id = createItem("store_one", item);

        CONTROLLER.creation(new MockHttpServletRequest(), "store_one", null, item);
        JSONObject actual = retrieveItem("store_one", id.toString());
        assertNotNull(actual);
        assertEquals(id.toString(), actual.getString("id"));

        CONTROLLER.delete(new MockHttpServletRequest(), "store_one", id.toString());

        retrieveItem("store_one", id.toString());
    }

    @Test
    public void storeStore() throws Exception {
        helper.setEnvIsAdmin(true);

        JSONObject store = new JSONObject();
        store.put("label", "item one");
        store.put("identity", 354);
        store.put("description", "this is some really long description, really really long, i mean it... ok, could be longer");
        store.put("isMarkedByABooleanFlag", true);

        CONTROLLER.creation(new MockHttpServletRequest(), "store_one", store, null);

        int numberOfStores = 0;
        for (JSONObject actual : retrieveStores()) {
            numberOfStores++;
            assertNotNull(actual);
            assertEquals("store_one", actual.getString("id"));
            assertEquals(store.getString("label"), actual.getString("label"));
            assertEquals(store.getInt("identity"), actual.getInt("identity"));
            assertEquals(store.getString("description"), actual.getString("description"));
            assertEquals(store.getBoolean("isMarkedByABooleanFlag"), actual.getBoolean("isMarkedByABooleanFlag"));
        }
        assertEquals(1, numberOfStores);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void create_notAdmin() throws Exception {
        CONTROLLER.creation(new MockHttpServletRequest(), "store_one", null, null);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void update_notAdmin() throws Exception {
        CONTROLLER.update(new MockHttpServletRequest(), "store_one", "354", null);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void delete_notAdmin() throws Exception {
        CONTROLLER.delete(new MockHttpServletRequest(), "store_one", "354");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void storeStore_notAdmin() throws Exception {
        CONTROLLER.creation(new MockHttpServletRequest(), "store_one", null, null);
    }

}

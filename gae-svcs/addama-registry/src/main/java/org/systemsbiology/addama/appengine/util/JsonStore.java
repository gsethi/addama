package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.datastore.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.appengine.datastore.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static java.util.UUID.randomUUID;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class JsonStore {
    private static final Logger log = Logger.getLogger(JsonStore.class.getName());

    private static final DatastoreService datastore = getDatastoreService();

    public static Iterable<JSONObject> retrieveStores() throws ResourceNotFoundException, JSONException {
        ArrayList<JSONObject> items = new ArrayList<JSONObject>();

        PreparedQuery pq = datastore.prepare(new Query("stores"));
        for (Entity e : pq.asIterable()) {
            JSONObject item = new JSONObject();
            String storeId = e.getKey().getName();
            item.put("id", storeId);
            fromEntity(e, item);
            if (!item.has("label")) {
                item.put("label", storeId);
            }
            items.add(item);
        }
        return items;
    }

    public static Iterable<JSONObject> retrieveItems(String storeId) throws ResourceNotFoundException, JSONException {
        ArrayList<JSONObject> items = new ArrayList<JSONObject>();

        PreparedQuery pq = datastore.prepare(new Query("stores-items", getStoreKey(storeId, false)));
        for (Entity e : pq.asIterable()) {
            JSONObject json = new JSONObject();
            json.put("id", e.getKey().getName());
            fromEntity(e, json);
            items.add(json);
        }

        return items;
    }

    public static JSONObject retrieveSchema(String storeId) {
        // TODO : Determine schema for store
        return new JSONObject();
    }

    public static JSONObject retrieveItem(String storeId, String itemId) throws ResourceNotFoundException, JSONException {
        Key storeKey = getStoreKey(storeId, false);
        Entity e;
        try {
            e = datastore.get(createKey(storeKey, "stores-items", itemId));
        } catch (EntityNotFoundException ex) {
            log.warning(ex.getMessage());
            throw new ResourceNotFoundException(itemId);
        }

        JSONObject item = new JSONObject();
        item.put("id", itemId);
        fromEntity(e, item);
        return item;
    }

    public static UUID createItem(String storeId, JSONObject item) throws JSONException, ResourceNotFoundException {
        UUID uuid = randomUUID();

        Key storeKey = getStoreKey(storeId, true);
        Key itemKey = createKey(storeKey, "stores-items", uuid.toString());

        Entity e = new Entity(itemKey);
        fromJson(item, e);

        inTransaction(datastore, new PutEntityTransactionCallback(e));
        return uuid;
    }

    public static void saveStore(String storeId, JSONObject item) throws JSONException, ResourceNotFoundException {
        Key storeKey = getStoreKey(storeId, true);
        Entity e = new Entity(storeKey);
        fromJson(item, e);
        inTransaction(datastore, new PutEntityTransactionCallback(e));
    }

    public static void updateItem(String storeId, String itemId, JSONObject item) throws ResourceNotFoundException, JSONException {
        Key itemKey = createKey(getStoreKey(storeId, true), "stores-items", itemId);
        Entity e;
        try {
            e = datastore.get(itemKey);
        } catch (EntityNotFoundException ex) {
            throw new ResourceNotFoundException(itemId);
        }

        fromJson(item, e);
        inTransaction(datastore, new PutEntityTransactionCallback(e));
    }

    public static void deleteItem(String storeId, String itemId) throws ResourceNotFoundException {
        Key itemKey = createKey(getStoreKey(storeId, false), "stores-items", itemId);
        inTransaction(datastore, new DeleteEntityTransactionCallback(itemKey));
    }

    /*
    * Private Methods
    */
    private static Key getStoreKey(String storeId, boolean createIfNotExists) throws ResourceNotFoundException {
        Key storeKey = createKey("stores", storeId);
        try {
            datastore.get(storeKey);
        } catch (EntityNotFoundException ex) {
            if (createIfNotExists) {
                log.info("creating new store:" + ex.getMessage());
                // TODO : Record user
                inTransaction(datastore, new PutEntityTransactionCallback(new Entity(storeKey)));
            } else {
                throw new ResourceNotFoundException(storeId);
            }
        }
        return storeKey;
    }

    private static void fromJson(JSONObject item, Entity e) throws JSONException {
        Iterator keys = item.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            e.setProperty(key, item.get(key));
        }
    }

    private static void fromEntity(Entity e, JSONObject item) {
        for (Map.Entry<String, Object> entry : e.getProperties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                item.put(key, value.toString());
            } catch (JSONException ex) {
                log.warning(key + ":" + value + ":" + ex.getMessage());
            }
        }
    }
}

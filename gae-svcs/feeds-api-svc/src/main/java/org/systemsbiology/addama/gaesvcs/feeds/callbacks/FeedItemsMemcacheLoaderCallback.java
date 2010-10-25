package org.systemsbiology.addama.gaesvcs.feeds.callbacks;

import com.google.appengine.api.datastore.*;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;

import java.io.Serializable;

/**
 * @author hrovira
 */
public class FeedItemsMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    public Serializable getCacheableObject(String feedUri) throws Exception {
        JSONObject json = new JSONObject();

        Key pk = KeyFactory.createKey("feeds", feedUri);
        PreparedQuery pq = datastoreService.prepare(new Query("feed-item", pk));
        for (Entity entity : pq.asIterable()) {
            JSONObject item = new JSONObject();
            item.put("author", entity.getProperty("author").toString());
            item.put("text", entity.getProperty("text").toString());
            item.put("date", entity.getProperty("date").toString());
            json.append("items", item);
        }

        return json.toString();
    }
}

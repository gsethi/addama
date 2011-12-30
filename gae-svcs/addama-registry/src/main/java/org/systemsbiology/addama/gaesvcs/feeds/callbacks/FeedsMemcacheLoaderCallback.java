package org.systemsbiology.addama.gaesvcs.feeds.callbacks;

import com.google.appengine.api.datastore.*;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;

import java.io.Serializable;

/**
 * @author hrovira
 */
public class FeedsMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    public Serializable getCacheableObject(String feedUri) throws Exception {
        JSONObject json = new JSONObject();

        PreparedQuery pq = datastoreService.prepare(new Query("feeds"));
        for (Entity entity : pq.asIterable()) {
            JSONObject item = new JSONObject();
            item.put("uri", entity.getKey().getName());
            item.put("creator", entity.getProperty("creator").toString());
            json.append("items", item);
        }
        return json.toString();
    }
}

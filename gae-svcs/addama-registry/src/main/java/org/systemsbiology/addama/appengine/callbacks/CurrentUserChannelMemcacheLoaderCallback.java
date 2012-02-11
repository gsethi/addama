package org.systemsbiology.addama.appengine.callbacks;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;

import java.io.Serializable;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static org.systemsbiology.addama.appengine.util.Channels.newChannelForUser;

/**
 * @author hrovira
 */
public class CurrentUserChannelMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(CurrentUserChannelMemcacheLoaderCallback.class.getName());

    public Serializable getCacheableObject(String channelId) throws Exception {
        DatastoreService datastore = getDatastoreService();

        try {
            Entity e = datastore.get(createKey("channels", channelId));
            return e.getProperty("token").toString();
        } catch (EntityNotFoundException e) {
            log.warning("token not found for " + channelId + ":" + e.getMessage());
        }

        return newChannelForUser();
    }
}

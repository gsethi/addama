package org.systemsbiology.addama.appengine.callbacks;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;

import java.io.Serializable;
import java.util.logging.Logger;

import static com.google.appengine.api.channel.ChannelServiceFactory.getChannelService;
import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class ChannelMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(ChannelMemcacheLoaderCallback.class.getName());

    public Serializable getCacheableObject(String userEmail) throws Exception {
        DatastoreService datastore = getDatastoreService();

        Key k = createKey("channels", userEmail);
        try {
            Entity e = datastore.get(k);
            return e.getProperty("token").toString();
        } catch (EntityNotFoundException e) {
            log.warning("token not found for " + userEmail + ":" + e.getMessage());
        }

        String token = getChannelService().createChannel(userEmail);

        Entity e = new Entity(createKey("channels", userEmail));
        e.setProperty("token", token);
        inTransaction(datastore, new PutEntityTransactionCallback(e));
        return token;
    }
}

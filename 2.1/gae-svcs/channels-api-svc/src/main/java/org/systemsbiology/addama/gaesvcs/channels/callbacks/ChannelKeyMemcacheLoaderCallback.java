package org.systemsbiology.addama.gaesvcs.channels.callbacks;

import com.google.appengine.api.datastore.*;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;

import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class ChannelKeyMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(ChannelKeyMemcacheLoaderCallback.class.getName());

    private final DatastoreService datastoreService;

    public ChannelKeyMemcacheLoaderCallback(DatastoreService datastoreService) {
        this.datastoreService = datastoreService;
    }

    public Serializable getCacheableObject(String key) throws Exception {
        Key k = KeyFactory.createKey("channels", key);

        try {
            Entity e = datastoreService.get(k);
            return e.getProperty("key").toString();
        } catch (EntityNotFoundException e) {
            log.warning("create new key for " + key + ":" + e.getMessage());
        }

        String channelKey = UUID.randomUUID().toString();

        Entity e = new Entity(k);
        e.setProperty("key", channelKey);
        inTransaction(datastoreService, new PutEntityTransactionCallback(e));
        return channelKey;
    }
}

package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import org.systemsbiology.addama.appengine.datastore.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;

import java.io.Serializable;
import java.util.logging.Logger;

import static com.google.appengine.api.channel.ChannelServiceFactory.getChannelService;
import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServiceTemplate.loadIfNotExisting;

/**
 * @author hrovira
 */
public class Channels {
    private static final Logger log = Logger.getLogger(Channels.class.getName());
    private static final MemcacheService channelKeys = getMemcacheService("channelkeys");
    private static final DatastoreService datastore = getDatastoreService();
    private static final Expiration expiration = byDeltaSeconds(3600);

    public static String myChannelToken() throws Exception {
        String userEmail = getUserService().getCurrentUser().getEmail();
        return (String) loadIfNotExisting(channelKeys, userEmail, new ChannelMemcacheLoaderCallback(), expiration);
    }

    public static void dropChannel(ChannelPresence presence) {
        String clientId = presence.clientId();
        log.info("clientId=" + clientId);
        channelKeys.delete(clientId);
        inTransaction(datastore, new DeleteEntityTransactionCallback(createKey("channels", clientId)));
    }

    /*
    * Private Classes
    */
    private static class ChannelMemcacheLoaderCallback implements MemcacheLoaderCallback {
        public Serializable getCacheableObject(String userEmail) throws Exception {
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
}

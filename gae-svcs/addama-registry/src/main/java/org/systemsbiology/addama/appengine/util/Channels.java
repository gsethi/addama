package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import org.json.JSONObject;
import org.systemsbiology.addama.appengine.callbacks.CurrentUserChannelMemcacheLoaderCallback;
import org.systemsbiology.addama.appengine.datastore.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;

import java.util.logging.Logger;

import static com.google.appengine.api.channel.ChannelServiceFactory.getChannelService;
import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
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
        String email = getUserService().getCurrentUser().getEmail();
        return (String) loadIfNotExisting(channelKeys, email, new CurrentUserChannelMemcacheLoaderCallback(), expiration);
    }

    public static void publishToAdmins(JSONObject json) {
        PreparedQuery pq = datastore.prepare(new Query("channels").addFilter("isAdmin", EQUAL, true));
        for (Entity e : pq.asIterable()) {
            String channelId = e.getKey().getName();
            getChannelService().sendMessage(new ChannelMessage(channelId, json.toString()));
        }
    }

    public static void publishMessage(String channelId, JSONObject json) {
        try {
            Entity e = datastore.get(createKey("channels", channelId));
            if (e != null) {
                getChannelService().sendMessage(new ChannelMessage(channelId, json.toString()));
            }
        } catch (EntityNotFoundException e) {
            log.warning("channel not found for " + channelId + ":" + e.getMessage());
        }
    }

    public static void dropChannel(ChannelPresence presence) {
        String clientId = presence.clientId();
        log.info("clientId=" + clientId);
        channelKeys.delete(clientId);
        inTransaction(datastore, new DeleteEntityTransactionCallback(createKey("channels", clientId)));
    }

    public static String newChannelForUser() {
        String email = getUserService().getCurrentUser().getEmail();
        boolean isAdmin = getUserService().isUserAdmin();

        String token = getChannelService().createChannel(email);

        Entity e = new Entity(createKey("channels", email));
        e.setProperty("isAdmin", isAdmin);
        e.setProperty("token", token);
        inTransaction(datastore, new PutEntityTransactionCallback(e));
        return token;
    }
}

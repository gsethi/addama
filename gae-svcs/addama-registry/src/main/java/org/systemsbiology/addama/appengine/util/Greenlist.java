package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import org.systemsbiology.addama.appengine.callbacks.GreenlistMemcacheLoaderCallback;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServiceTemplate.loadIfNotExisting;

/**
 * @author aeakin
 */
public class Greenlist {
    private static final Logger log = Logger.getLogger(Greenlist.class.getName());
    public static final String BOUNCER = "bouncer@addama.org";

    public static boolean isGreenlistActive() {
        return isGreenlisted(BOUNCER);
    }

    public static boolean isGreenlisted(String email) {
        if (isEmpty(email)) {
            return false;
        }
        try {
            MemcacheLoaderCallback callback = new GreenlistMemcacheLoaderCallback();
            return (Boolean) loadIfNotExisting(getMemcacheService("greenlist"), email, callback, byDeltaSeconds(3600));
        } catch (Exception e) {
            log.warning(email + ":" + e);
        }
        return false;
    }

    public static Iterable<String> getGreenlist() {
        ArrayList<String> userEmails = new ArrayList<String>();

        Iterator<Entity> itr = getDatastoreService().prepare(new Query("greenlist")).asIterator();
        while (itr.hasNext()) {
            String userEmail = itr.next().getKey().getName();
            if (equalsIgnoreCase(userEmail, BOUNCER)) {
                userEmails.add(userEmail);
            }
        }

        return userEmails;
    }

    public static void addGreenlistUsers(Iterable<String> users) {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        entities.add(new Entity(createKey("greenlist", BOUNCER)));
        for (String user : users) {
            entities.add(new Entity(createKey("greenlist", user.toLowerCase())));
        }
        inTransaction(getDatastoreService(), new PutEntityTransactionCallback(entities));
    }
}

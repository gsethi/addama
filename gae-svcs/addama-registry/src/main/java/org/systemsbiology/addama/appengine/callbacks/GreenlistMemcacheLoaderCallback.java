package org.systemsbiology.addama.appengine.callbacks;

import com.google.appengine.api.datastore.EntityNotFoundException;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;

import java.io.Serializable;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;

/**
 * @author hrovira
 */
public class GreenlistMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(GreenlistMemcacheLoaderCallback.class.getName());

    public Serializable getCacheableObject(String userEmail) throws Exception {
        try {
            if (getDatastoreService().get(createKey("greenlist", userEmail.toLowerCase())) != null) {
                return true;
            }
        } catch (EntityNotFoundException ex) {
            log.warning("user not in greenlist:" + ex);
        }
        return false;
    }

}

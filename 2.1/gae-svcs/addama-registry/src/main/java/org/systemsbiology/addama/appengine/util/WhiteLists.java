package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.MemcacheService;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.coresvcs.gae.pojos.WhiteListEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static java.lang.Boolean.parseBoolean;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author aeakin
 */
public class WhiteLists {
    private static final Logger log = Logger.getLogger(WhiteLists.class.getName());

    private final static DatastoreService datastore = getDatastoreService();
    private final static MemcacheService memcache = getMemcacheService();

    public static boolean isUserInWhiteList(String userUri, String accessPath) {
        log.fine(userUri + "," + accessPath);

        try {
            String tempUri = accessPath;
            //if there are no users in the white list, then move up the stack of uri's and see if there
            //is one to query
            int entityCount = 0;
            while (!tempUri.equals("")) {
                //first check memcache - if entry is there then the user has access
                if (memcache.contains(tempUri)) {
                    entityCount = (Integer) memcache.get(tempUri);
                } else {
                    Query q = new Query("white-list");
                    q.addFilter("uri", Query.FilterOperator.EQUAL, tempUri);
                    PreparedQuery pq = datastore.prepare(q);
                    entityCount = pq.countEntities();
                }

                if (entityCount != 0)
                    break;

                tempUri = StringUtils.substringBeforeLast(tempUri, "/");
            }

            if (tempUri.equals(""))
                return true;

            //check and see if user is in this white list
            if (memcache.contains(userUri + tempUri)) {
                Entity e = (Entity) memcache.get(userUri + tempUri);
                return hasAccess(e);
            }

            //user not found in cache, so get out of datastore and put in cache
            Entity e = datastore.get(createKey("white-list", userUri + tempUri));
            if (e != null) {
                memcache.put(userUri + tempUri, e);
                return true;
            } else {
                memcache.put(userUri + tempUri, null);
                return false;
            }
        } catch (EntityNotFoundException e) {
            log.warning(userUri + "," + accessPath + ":" + e);
            return false;
        }
    }

    public static WhiteListEntry[] getWhiteListUsers() {
        Query q = new Query("white-list");

        ArrayList<WhiteListEntry> userEmails = new ArrayList<WhiteListEntry>();
        PreparedQuery pq = datastore.prepare(q);
        Iterator<Entity> itr = pq.asIterator();
        while (itr.hasNext()) {
            String accessPath = null;
            String userEmail = null;

            Entity e = itr.next();
            if (e.hasProperty("uri")) {
                accessPath = e.getProperty("uri").toString();
            }
            if (e.hasProperty("userUri")) {
                String userUri = e.getProperty("userUri").toString();
                userEmail = StringUtils.substringAfterLast(userUri, "/");
            }

            userEmails.add(new WhiteListEntry(userEmail, accessPath, hasAccess(e)));
        }

        return userEmails.toArray(new WhiteListEntry[userEmails.size()]);
    }

    public static void addWhiteListUser(String userEmail, String accessPath) {
        log.fine(userEmail + "," + accessPath);

        String userUri = "/addama/users/" + userEmail;
        Entity e = new Entity(createKey("white-list", userUri + accessPath));
        e.setProperty("hasAccess", false);
        e.setProperty("uri", accessPath);
        e.setProperty("userUri", userUri);
        inTransaction(datastore, new PutEntityTransactionCallback(e));

    }

    public static void deleteWhiteListUser(String userEmail, String accessPath) {
        log.fine(userEmail + "," + accessPath);

        if (!StringUtils.isEmpty(userEmail) && !StringUtils.isEmpty(accessPath)) {
            Key k = createKey("white-list", "/addama/users/" + userEmail + accessPath);
            inTransaction(datastore, new DeleteEntityTransactionCallback(k));
        }

    }

    public static void grantWhiteListAccess(String userEmail, String accessPath) {
        log.fine(userEmail + "," + accessPath);

        Entity e = new Entity(createKey("white-list", "/addama/users/" + userEmail + accessPath));
        e.setProperty("hasAccess", true);
        e.setProperty("uri", accessPath);
        e.setProperty("userUri", "/addama/users/" + userEmail);
        inTransaction(datastore, new PutEntityTransactionCallback(e));
    }

    /*
     * Private Methods
     */

    private static boolean hasAccess(Entity e) {
        if (e == null || !e.hasProperty("hasAccess")) {
            return false;
        }

        return parseBoolean(e.getProperty("hasAccess").toString());
    }
}

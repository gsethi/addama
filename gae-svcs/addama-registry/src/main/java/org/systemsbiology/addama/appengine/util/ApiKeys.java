package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import org.systemsbiology.addama.appengine.pojos.ApiKey;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static java.lang.Boolean.parseBoolean;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class ApiKeys {
    private static final Logger log = Logger.getLogger(ApiKeys.class.getName());

    private static final DatastoreService datastore = getDatastoreService();
    private static final UserService userService = getUserService();

    public static String getUserUriFromApiKey(String apikey) {
        if (!isEmpty(apikey)) {
            try {
                Entity e = datastore.get(createKey("api-keys", apikey));
                return "/addama/users/" + e.getProperty("user").toString();
            } catch (Exception e) {
                log.warning(apikey + ":" + e);
            }
        }
        return null;
    }

    public static boolean isAdmin(String apiKey) {
        if (!isEmpty(apiKey)) {
            try {
                Entity e = datastore.get(createKey("api-keys", apiKey));
                if (e.hasProperty("isAdmin")) {
                    return parseBoolean(e.getProperty("isAdmin").toString());
                }
            } catch (Exception e) {
                log.warning(apiKey + ":" + e);
            }
        }
        return false;
    }

    public static boolean isValid(String apiKey, String remoteAddr) {
        log.fine(apiKey + "," + remoteAddr);
        try {
            Key k = createKey("api-keys", apiKey);
            return datastore.get(k) != null;
        } catch (EntityNotFoundException e) {
            log.warning(apiKey + ":" + e);
        }
        return false;
    }

    public static ApiKey getUserApiKey() throws ForbiddenAccessException {
        Query q = new Query("api-keys");

        User user = getCurrentUser();
        q.addFilter("user", Query.FilterOperator.EQUAL, user.getEmail());

        PreparedQuery pq = datastore.prepare(q);
        Iterator<Entity> itr = pq.asIterator();
        if (itr.hasNext()) {
            Entity e = itr.next();
            String apikey = e.getKey().getName();
            String userId = e.getProperty("user").toString();

            // TODO : this is not needed
            boolean admin = false;
            if (e.hasProperty("isAdmin")) {
                admin = parseBoolean(e.getProperty("isAdmin").toString());
            }
            return new ApiKey(userId, fromString(apikey), admin);
        }

        UUID uuid = randomUUID();

        boolean isAdmin = userService.isUserAdmin();

        Entity e = new Entity(createKey("api-keys", uuid.toString()));
        e.setProperty("user", user.getEmail());
        e.setProperty("isAdmin", isAdmin);

        inTransaction(datastore, new PutEntityTransactionCallback(e));

        return new ApiKey("/addama/users/" + user.getEmail(), uuid, isAdmin);
    }

}

/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.coresvcs.gae.services;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.coresvcs.gae.pojos.ApiKey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ApiKeys {
    private static final Logger log = Logger.getLogger(ApiKeys.class.getName());

    private DatastoreServiceTemplate template;

    public void setTemplate(DatastoreServiceTemplate template) {
        this.template = template;
    }

    /*
     * Public Methods
     */

    public boolean isValidKey(String apiKey, String clientIP) {
        log.fine("isValidKey(" + apiKey + "," + clientIP + ")");
        try {
            Key k = KeyFactory.createKey("api-keys", apiKey);
            return template.getEntityByKey(k) != null;
        } catch (EntityNotFoundException e) {
            log.warning("isValidKey(" + apiKey + "):" + e);
            return false;
        }
    }

    public ApiKey generateKey() throws ForbiddenAccessException {
        log.fine("generateKey()");

        User user = getCurrentUser();
        UUID uuid = UUID.randomUUID();

        String userUri = "/addama/users/" + user.getEmail();
        boolean isAdmin = UserServiceFactory.getUserService().isUserAdmin();

        Entity e = new Entity(KeyFactory.createKey("api-keys", uuid.toString()));
        e.setProperty("user", userUri);
        e.setProperty("isAdmin", isAdmin);

        template.inTransaction(new PutEntityTransactionCallback(e));

        return new ApiKey(userUri, uuid, isAdmin);
    }

    public ApiKey[] getApiKeysForCurrentUser() throws ForbiddenAccessException {
        User user = getCurrentUser();
        Query q = new Query("api-keys").addFilter("user", Query.FilterOperator.EQUAL, "/addama/users/" + user.getEmail());

        ArrayList<ApiKey> apiKeys = new ArrayList<ApiKey>();

        PreparedQuery pq = template.prepare(q);
        Iterator<Entity> itr = pq.asIterator();
        while (itr.hasNext()) {
            Entity e = itr.next();
            String apikey = e.getKey().getName();

            boolean admin = false;
            if (e.hasProperty("isAdmin")) {
                admin = Boolean.parseBoolean(e.getProperty("isAdmin").toString());
            }
            apiKeys.add(new ApiKey(e.getProperty("user").toString(), UUID.fromString(apikey), admin));
        }
        return apiKeys.toArray(new ApiKey[apiKeys.size()]);
    }

    public ApiKey[] getApiKeys() throws ForbiddenAccessException {
        log.fine("getApiKeys()");

        Query q = new Query("api-keys");
        if (!UserServiceFactory.getUserService().isUserAdmin()) {
            User user = getCurrentUser();
            q.addFilter("user", Query.FilterOperator.EQUAL, "/addama/users/" + user.getEmail());
        }

        ArrayList<ApiKey> apiKeys = new ArrayList<ApiKey>();

        PreparedQuery pq = template.prepare(q);
        Iterator<Entity> itr = pq.asIterator();
        while (itr.hasNext()) {
            Entity e = itr.next();
            String apikey = e.getKey().getName();
            String user = "not assigned";
            if (e.hasProperty("user")) {
                user = e.getProperty("user").toString();
            }
            boolean admin = false;
            if (e.hasProperty("isAdmin")) {
                admin = Boolean.parseBoolean(e.getProperty("isAdmin").toString());
            }
            apiKeys.add(new ApiKey(user, UUID.fromString(apikey), admin));
        }
        return apiKeys.toArray(new ApiKey[apiKeys.size()]);
    }

    public String getUserUriFromApiKey(String apikey) {
        log.fine("getUserUriFromApiKey(" + apikey + ")");
        if (!StringUtils.isEmpty(apikey)) {
            try {
                Entity e = template.getEntityByKey(KeyFactory.createKey("api-keys", apikey));
                if (e.hasProperty("user")) {
                    return e.getProperty("user").toString();
                }
            } catch (Exception e) {
                log.warning("getUserUriFromApiKey(" + apikey + "):" + e);
            }
        }
        return null;
    }

    public boolean isKeyAdmin(String apikey) {
        log.fine("isKeyAdmin(" + apikey + ")");
        if (!StringUtils.isEmpty(apikey)) {
            try {
                Entity e = template.getEntityByKey(KeyFactory.createKey("api-keys", apikey));
                if (e.hasProperty("isAdmin")) {
                    return Boolean.parseBoolean(e.getProperty("isAdmin").toString());
                }
            } catch (Exception e) {
                log.warning("isKeyAdmin(" + apikey + "):" + e);
            }
        }
        return false;
    }

    public void deleteApiKey(String apiKey) {
        log.fine("deleteApiKey(" + apiKey + ")");
        if (!StringUtils.isEmpty(apiKey)) {
            template.inTransaction(new DeleteEntityTransactionCallback(KeyFactory.createKey("api-keys", apiKey)));
        }
    }

    /*
     * Private Methods
     */

    private User getCurrentUser() throws ForbiddenAccessException {
        UserService userService = UserServiceFactory.getUserService();
        if (!userService.isUserLoggedIn()) {
            throw new ForbiddenAccessException();
        }

        User user = userService.getCurrentUser();
        if (user == null) {
            throw new ForbiddenAccessException();
        }

        return user;
    }

}

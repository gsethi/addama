package org.systemsbiology.addama.appengine.entities;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.UUID;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class JsonEntityStore {
    private final DatastoreService datastore = getDatastoreService();

    public static void newObjectDomain(String uri) throws InvalidSyntaxException, ResourceNotFoundException, ForbiddenAccessException {
        if (isEmpty(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        if (getUserService().isUserLoggedIn() && getUserService().isUserAdmin()) {

            return;
        }

        throw new ForbiddenAccessException(getUserService().getCurrentUser().getEmail());
    }

    public static JSONObject getJson(String uri) throws JSONException {
        // TODO : Determine if retrieving domain, or object
        return new JSONObject().put("uri", uri);
    }

    public static String getObjectDomain(String uri) throws ResourceNotFoundException {
        for (String objectDomain : getObjectDomains()) {
            if (equalsIgnoreCase(objectDomain, uri)) {
                return objectDomain;
            }
            if (uri.startsWith(objectDomain)) {
                return objectDomain;
            }
        }
        throw new ResourceNotFoundException(uri);
    }

    public static String[] getObjectDomains() {
        ArrayList<String> objectDomains = new ArrayList<String>();
        return objectDomains.toArray(new String[objectDomains.size()]);
    }

    public static UUID create(String objectDomain, JSONObject json) throws InvalidSyntaxException {
        if (isEmpty(objectDomain)) {
            throw new InvalidSyntaxException("invalid uri");
        }
        if (json == null) {
            throw new InvalidSyntaxException("invalid json");
        }

        return randomUUID();
    }

    public static JSONObject retrieve(String uri) throws ResourceNotFoundException, JSONException {
        if (isEmpty(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        return new JSONObject().put("uri", uri);
    }

    public static void update(String uri, JSONObject json) throws ResourceNotFoundException, InvalidSyntaxException {
        if (isEmpty(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        if (json == null) {
            throw new InvalidSyntaxException("no json content submitted");
        }
    }

    public static void delete(String uri) throws ResourceNotFoundException {
        if (isEmpty(uri)) {
            throw new ResourceNotFoundException(uri);
        }
    }
}

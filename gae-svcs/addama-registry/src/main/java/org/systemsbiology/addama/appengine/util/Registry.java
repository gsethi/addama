package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.datastore.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.AntPathMatcher;
import org.systemsbiology.addama.appengine.pojos.RegistryMapping;
import org.systemsbiology.addama.appengine.pojos.RegistryService;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class Registry {
    private static final Logger log = Logger.getLogger(Registry.class.getName());

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final DatastoreService datastore = getDatastoreService();

    /*
     * Services
     */

    public static RegistryService[] getRegistryServices() {
        ArrayList<RegistryService> registryServices = new ArrayList<RegistryService>();
        try {
            PreparedQuery pq = datastore.prepare(new Query("registry-services"));
            Iterator<Entity> itr = pq.asIterator();
            while (itr.hasNext()) {
                registryServices.add(getServiceFromEntity(itr.next()));
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return registryServices.toArray(new RegistryService[registryServices.size()]);
    }

    public static RegistryService[] getSearchableServices() {
        ArrayList<RegistryService> searchableServices = new ArrayList<RegistryService>();

        Query q = new Query("registry-services").addFilter("searchable", Query.FilterOperator.EQUAL, true);
        PreparedQuery pq = datastore.prepare(q);
        for (Entity e : pq.asIterable()) {
            if (e.hasProperty("REGISTRY_SERVICE_KEY") && e.hasProperty("url")) {
                try {
                    searchableServices.add(getServiceFromEntity(e));
                } catch (MalformedURLException ex) {
                    log.warning("getSearchableServices():" + ex);
                }
            }
        }

        return searchableServices.toArray(new RegistryService[searchableServices.size()]);
    }

    public static RegistryService getRegistryService(String serviceUri) {
        if (!isEmpty(serviceUri)) {
            try {
                Key k = createKey("registry-services", serviceUri);
                Entity e = datastore.get(k);
                if (e.hasProperty("REGISTRY_SERVICE_KEY") && e.hasProperty("url")) {
                    return getServiceFromEntity(e);
                }
            } catch (Exception e) {
                log.warning("getRegistryService(" + serviceUri + "):" + e);
            }
        }

        return null;
    }

    public static RegistryService getRegistryServiceByItsUri(String requestUri) {
        if (!isEmpty(requestUri)) {
            for (RegistryService service : getRegistryServices()) {
                if (requestUri.startsWith(service.getUri())) {
                    return service;
                }
            }
        }
        return null;
    }

    /*
     * Mappings
     */

    public static RegistryMapping[] getMatchingRegistryMappings(String requestUri) {
        HashMap<String, RegistryMapping> mappings = new HashMap<String, RegistryMapping>();

        PreparedQuery pq = datastore.prepare(new Query("registry-mappings"));
        for (Entity entity : pq.asIterable()) {
            if (isMatchingService(requestUri, entity)) {
                RegistryMapping rm = getMappingFromEntity(entity);
                mappings.put(rm.getServiceUri(), rm);
            }
        }

        return mappings.values().toArray(new RegistryMapping[mappings.size()]);
    }

    public static RegistryMapping[] getRegistryMappingsByItsUri(String requesturi) {
        ArrayList<RegistryMapping> mappings = new ArrayList<RegistryMapping>();
        try {
            Query q = new Query("registry-mappings");
            PreparedQuery pq = datastore.prepare(q);
            Iterator<Entity> itr = pq.asIterator();
            while (itr.hasNext()) {
                Entity e = itr.next();
                if (e.hasProperty("uri")) {
                    String uri = e.getProperty("uri").toString();
                    if (uri.startsWith(requesturi)) {
                        mappings.add(getMappingFromEntity(e));
                    }
                }
            }
        } catch (Exception e) {
            log.warning(requesturi + ":" + e);
        }
        return mappings.toArray(new RegistryMapping[mappings.size()]);
    }

    public static RegistryMapping[] getRegistryMappingFamily(String familyUri) {
        ArrayList<RegistryMapping> mappings = new ArrayList<RegistryMapping>();
        try {
            Query q = new Query("registry-mappings");
            PreparedQuery pq = datastore.prepare(q.addFilter("family", Query.FilterOperator.EQUAL, familyUri));
            Iterator<Entity> itr = pq.asIterator();
            while (itr.hasNext()) {
                mappings.add(getMappingFromEntity(itr.next()));
            }
        } catch (Exception e) {
            log.warning(familyUri + ":" + e);
        }
        return mappings.toArray(new RegistryMapping[mappings.size()]);
    }

    public static RegistryMapping[] getRegistryMappings(RegistryService registryService) {
        ArrayList<RegistryMapping> mappings = new ArrayList<RegistryMapping>();
        Query q = new Query("registry-mappings").addFilter("service", Query.FilterOperator.EQUAL, registryService.getUri());
        PreparedQuery pq = datastore.prepare(q);
        for (Entity e : pq.asIterable()) {
            mappings.add(getMappingFromEntity(e));
        }
        return mappings.toArray(new RegistryMapping[mappings.size()]);
    }

    public static RegistryMapping getStaticContentRegistryMapping(String requestUri) {
        Query q = new Query("registry-mappings").addFilter("staticContent", Query.FilterOperator.EQUAL, true);
        PreparedQuery pq = datastore.prepare(q);
        for (Entity entity : pq.asIterable()) {
            if (isMatchingService(requestUri, entity)) {
                return getMappingFromEntity(entity);
            }
        }

        return null;
    }

    public static JSONObject toJSON(RegistryService registryService) throws JSONException {
        if (registryService == null) {
            return null;
        }

        JSONObject json = new JSONObject();
        json.put("url", registryService.getUrl());
        json.put("label", registryService.getLabel());
        json.put("uri", registryService.getUri());
        return json;
    }

    /*
    * Persistence
    */
    public static void clearExistingService(String serviceId) {
        Query q = new Query("registry-mappings").addFilter("service", EQUAL, serviceId);
        PreparedQuery pq = datastore.prepare(q);
        ArrayList<Key> keys = new ArrayList<Key>();
        keys.add(createKey("registry-services", serviceId));
        for (Entity e : pq.asIterable()) {
            keys.add(e.getKey());
        }
        inTransaction(datastore, new DeleteEntityTransactionCallback(keys));
    }

    public static Entity newServiceEntity(String serviceId, String registryKey, JSONObject json) throws Exception {
        String serviceHost = json.getString("url");

        Entity e = new Entity(createKey("registry-services", serviceId));
        e.setProperty("url", new URL(serviceHost).toString());
        e.setProperty("label", json.getString("label"));
        if (json.has("searchable")) {
            e.setProperty("searchable", json.getBoolean("searchable"));
        }
        if (json.has("sharing")) {
            e.setProperty("sharing", json.getString("sharing"));
        }

        e.setUnindexedProperty("REGISTRY_SERVICE_KEY", registryKey);
        return e;
    }

    public static Entity newMappingEntity(String serviceId, JSONObject json) throws JSONException {
        String uri = json.getString("uri");

        Entity e = new Entity(createKey("registry-mappings", uri));
        e.setProperty("service", serviceId);
        e.setProperty("uri", uri);
        e.setProperty("label", json.getString("label"));
        if (json.has("pattern")) {
            e.setProperty("pattern", json.getString("pattern"));
        } else {
            e.setProperty("pattern", uri + "/**");
        }
        if (json.has("family")) {
            e.setProperty("family", json.getString("family"));
        } else {
            e.setProperty("family", "/addama/" + substringBetween(uri, "/addama/", "/"));
        }
        return e;
    }


    /*
    * Private Methods
    */

    private static boolean isMatchingService(String requestUri, Entity entity) {
        if (entity.hasProperty("service")) {
            if (entity.hasProperty("pattern")) {
                String pattern = entity.getProperty("pattern").toString();
                if (pathMatcher.match(pattern, requestUri)) {
                    log.info(requestUri + ": matches! [" + pattern + "]");
                    return true;
                }
            }
        }
        return false;
    }

    private static RegistryMapping getMappingFromEntity(Entity e) {
        RegistryMapping rm = new RegistryMapping();
        rm.setUri(e.getProperty("uri").toString());
        rm.setServiceUri(e.getProperty("service").toString());
        if (e.hasProperty("label")) {
            rm.setLabel(e.getProperty("label").toString());
        } else {
            rm.setLabel("Not Labeled");
        }
        if (e.hasProperty("handleAsynch")) {
            try {
                rm.setHandleAsynch(parseBoolean(e.getProperty("handleAsynch").toString()));
            } catch (Exception e1) {
                rm.setHandleAsynch(false);
            }
        }
        return rm;
    }

    private static RegistryService getServiceFromEntity(Entity e) throws MalformedURLException {
        RegistryService rs = new RegistryService();
        rs.setUri(e.getKey().getName());
        rs.setUrl(new URL(e.getProperty("url").toString()));
        rs.setAccessKey(UUID.fromString(e.getProperty("REGISTRY_SERVICE_KEY").toString()));
        if (e.hasProperty("sharing")) {
            rs.setSharingUri(e.getProperty("sharing").toString());
        }
        if (e.hasProperty("label")) {
            rs.setLabel(e.getProperty("label").toString());
        }
        if (e.hasProperty("searchable")) {
            rs.setSearchable(Boolean.parseBoolean(e.getProperty("searchable").toString()));
        }
        return rs;
    }
}

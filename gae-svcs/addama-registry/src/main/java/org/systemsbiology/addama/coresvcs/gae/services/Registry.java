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
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.PathMatcher;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryMapping;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author hrovira
 * @todo Memcache!
 */
public class Registry {
    private static final Logger log = Logger.getLogger(Registry.class.getName());

    private DatastoreServiceTemplate template;
    private PathMatcher pathMatcher;

    public void setTemplate(DatastoreServiceTemplate template) {
        this.template = template;
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    /*
     * Request Mapping Methods
     */

    public void setRegistryMapping(String mappingUri, JSONObject json) throws JSONException {
        log.fine("setRegistryMapping(" + mappingUri + "," + json + ")");

        String uri = json.getString("uri");
        if (!json.has("family")) {
            json.put("family", "/addama/" + StringUtils.substringBetween(uri, "/addama/", "/"));
        }
        if (!json.has("pattern")) {
            json.put("pattern", uri + "/**");
        }

        Entity e = new Entity(KeyFactory.createKey("registry-mappings", mappingUri));
        for (String required : new String[]{"service", "pattern", "uri"}) {
            e.setProperty(required, json.getString(required));
        }
        for (String optional : new String[]{"label", "family"}) {
            if (json.has(optional)) {
                e.setProperty(optional, json.getString(optional));
            }
        }
        if (json.has("staticContent")) {
            e.setProperty("staticContent", json.getBoolean("staticContent"));
        }

        template.inTransaction(new PutEntityTransactionCallback(e));
    }

    public void removeRegistryMapping(String mappingUri) throws JSONException {
        log.fine("setRegistryMapping(" + mappingUri + ")");

        Key k = KeyFactory.createKey("registry-mappings", mappingUri);
        template.inTransaction(new DeleteEntityTransactionCallback(k));
    }

    /*
     * Request Service Methods
     */

    public UUID setRegistryService(String serviceUri, JSONObject json) throws JSONException, MalformedURLException {
        log.fine("setRegistryService(" + serviceUri + "," + json + ")");

        if (!StringUtils.isEmpty(serviceUri)) {
            Entity e = new Entity(KeyFactory.createKey("registry-services", serviceUri));
            e.setProperty("url", new URL(json.getString("url")).toString());
            if (json.has("label")) {
                e.setProperty("label", json.getString("label"));
            }
            if (json.has("searchable")) {
                e.setProperty("searchable", json.getBoolean("searchable"));
            }
            if (json.has("sharing")) {
                e.setProperty("sharing", json.getString("sharing"));
            }

            UUID uuid = UUID.randomUUID();
            e.setUnindexedProperty("REGISTRY_SERVICE_KEY", uuid.toString());
            template.inTransaction(new PutEntityTransactionCallback(e));
            return uuid;
        }
        return null;
    }

    public void removeRegistryService(String serviceUri) throws JSONException {
        log.fine("removeRegistryService(" + serviceUri + ")");

        Key k = KeyFactory.createKey("registry-services", serviceUri);
        template.inTransaction(new DeleteEntityTransactionCallback(k));
    }

    /*
     * Proxy Methods
     */

    public RegistryService getRegistryService(String serviceUri) {
        log.fine("getRegistryService(" + serviceUri + ")");
        if (!StringUtils.isEmpty(serviceUri)) {
            try {
                Key k = KeyFactory.createKey("registry-services", serviceUri);
                Entity e = template.getEntityByKey(k);
                if (e.hasProperty("REGISTRY_SERVICE_KEY") && e.hasProperty("url")) {
                    return getServiceFromEntity(e);
                }
            } catch (Exception e) {
                log.warning("getRegistryService(" + serviceUri + "):" + e);
            }
        }
        return null;
    }

    public RegistryService getMatchingService(String requestUri) {
        log.fine("getMatchingService(" + requestUri + ")");
        if (!StringUtils.isEmpty(requestUri)) {
            PreparedQuery pq = template.prepare(new Query("registry-services"));
            for (Entity entity : pq.asIterable()) {
                try {
                    String serviceUri = entity.getKey().getName();
                    if (requestUri.startsWith(serviceUri)) {
                        return getServiceFromEntity(entity);
                    }
                } catch (Exception e) {
                    log.warning("getMatchingService(" + requestUri + "):" + e);
                }
            }
        }
        return null;
    }

    public RegistryMapping getRegistryMapping(String requestUri) {
        log.fine("getRegistryMapping(" + requestUri + ")");

        PreparedQuery pq = template.prepare(new Query("registry-mappings"));
        for (Entity entity : pq.asIterable()) {
            if (isMatchingService(requestUri, entity)) {
                return getMappingFromEntity(entity);
            }
        }

        return null;
    }

    public RegistryMapping[] getRegistryMappings(String requestUri) {
        log.fine("getRegistryMapping(" + requestUri + ")");

        HashMap<String, RegistryMapping> mappings = new HashMap<String, RegistryMapping>();

        PreparedQuery pq = template.prepare(new Query("registry-mappings"));
        for (Entity entity : pq.asIterable()) {
            if (isMatchingService(requestUri, entity)) {
                RegistryMapping rm = getMappingFromEntity(entity);
                mappings.put(rm.getServiceUri(), rm);
            }
        }

        return mappings.values().toArray(new RegistryMapping[mappings.size()]);
    }

    public RegistryMapping getStaticContentRegistryMapping(String requestUri) {
        log.fine("getStaticContentRegistryMapping(" + requestUri + ")");

        Query q = new Query("registry-mappings").addFilter("staticContent", Query.FilterOperator.EQUAL, true);
        PreparedQuery pq = template.prepare(q);
        for (Entity entity : pq.asIterable()) {
            if (isMatchingService(requestUri, entity)) {
                return getMappingFromEntity(entity);
            }
        }

        return null;
    }

    public RegistryMapping[] getRegistryMappings(RegistryService registryService) {
        ArrayList<RegistryMapping> mappings = new ArrayList<RegistryMapping>();
        Query q = new Query("registry-mappings").addFilter("service", Query.FilterOperator.EQUAL, registryService.getUri());
        PreparedQuery pq = template.prepare(q);
        for (Entity e : pq.asIterable()) {
            mappings.add(getMappingFromEntity(e));
        }
        return mappings.toArray(new RegistryMapping[mappings.size()]);
    }

    public RegistryService[] getSearchableServices() {
        log.fine("getSearchableServices()");

        ArrayList<RegistryService> searchableServices = new ArrayList<RegistryService>();

        Query q = new Query("registry-services").addFilter("searchable", Query.FilterOperator.EQUAL, true);
        PreparedQuery pq = template.prepare(q);
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

    /*
     * Registry Browse Methods
     */

    public RegistryService[] getRegistryServices() {
        ArrayList<RegistryService> registryServices = new ArrayList<RegistryService>();
        try {
            PreparedQuery pq = template.prepare(new Query("registry-services"));
            Iterator<Entity> itr = pq.asIterator();
            while (itr.hasNext()) {
                registryServices.add(getServiceFromEntity(itr.next()));
            }
        } catch (Exception e) {
            log.throwing(getClass().getName(), "getRegistryServices", e);
        }
        return registryServices.toArray(new RegistryService[registryServices.size()]);
    }

    public RegistryMapping[] getRegistryMappingFamily(String familyUri) {
        ArrayList<RegistryMapping> mappings = new ArrayList<RegistryMapping>();
        try {
            Query q = new Query("registry-mappings");
            PreparedQuery pq = template.prepare(q.addFilter("family", Query.FilterOperator.EQUAL, familyUri));
            Iterator<Entity> itr = pq.asIterator();
            while (itr.hasNext()) {
                mappings.add(getMappingFromEntity(itr.next()));
            }
        } catch (Exception e) {
            log.throwing(getClass().getName(), familyUri, e);
        }
        return mappings.toArray(new RegistryMapping[mappings.size()]);
    }

    public RegistryMapping[] getRegistryMappingsByUri(String requestURI) {
        ArrayList<RegistryMapping> mappings = new ArrayList<RegistryMapping>();
        try {
            Query q = new Query("registry-mappings");
            PreparedQuery pq = template.prepare(q);
            Iterator<Entity> itr = pq.asIterator();
            while (itr.hasNext()) {
                Entity e = itr.next();
                if (e.hasProperty("uri")) {
                    String uri = e.getProperty("uri").toString();
                    if (uri.startsWith(requestURI)) {
                        mappings.add(getMappingFromEntity(e));
                    }
                }
            }
        } catch (Exception e) {
            log.throwing(getClass().getName(), requestURI, e);
        }
        return mappings.toArray(new RegistryMapping[mappings.size()]);
    }

    /*
     * Private Methods
     */

    private boolean isMatchingService(String requestUri, Entity entity) {
        log.fine("isMatchingService(" + requestUri + ")");
        if (entity.hasProperty("service")) {
            if (entity.hasProperty("pattern")) {
                String pattern = entity.getProperty("pattern").toString();
                if (pathMatcher.match(pattern, requestUri)) {
                    log.info("isMatchingService(" + requestUri + "): matches! [" + pattern + "]");
                    return true;
                }
            }
        }
        return false;
    }

    private RegistryMapping getMappingFromEntity(Entity e) {
        RegistryMapping rm = new RegistryMapping();
        rm.setUri(e.getProperty("uri").toString());
        rm.setServiceUri(e.getProperty("service").toString());
        if (e.hasProperty("label")) {
            rm.setLabel(e.getProperty("label").toString());
        } else {
            rm.setLabel("Not Labeled");
        }
        return rm;
    }

    private RegistryService getServiceFromEntity(Entity e) throws MalformedURLException {
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

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
package org.systemsbiology.addama.commons.gae.config;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;

import java.net.URL;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.urlfetch.HTTPMethod.POST;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.gae.Appspot.APPSPOT_URL;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class ServiceRegistrationJsonConfigHandler implements JsonConfigHandler {
    private static final Logger log = Logger.getLogger(ServiceRegistrationJsonConfigHandler.class.getName());

    private final URLFetchService urlfetch = getURLFetchService();
    private final DatastoreService datastore = getDatastoreService();

    private final String registryDomain;
    private final String apiKey;

    public ServiceRegistrationJsonConfigHandler(String registryDomain, String apiKey) {
        this.registryDomain = registryDomain;
        this.apiKey = apiKey;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("service")) {
            JSONObject service = configuration.getJSONObject("service");
            service.put("url", APPSPOT_URL);

            String serviceUri = service.getString("uri");
            String registerUri = replace(serviceUri, "/addama/services", "/addama/registry/services");
            String payload = "service=" + service.toString();

            URL url = new URL(registryDomain + registerUri);
            HTTPRequest httpRequest = new HTTPRequest(url, POST);
            httpRequest.setHeader(new HTTPHeader("x-addama-apikey", apiKey));
            httpRequest.setPayload(payload.getBytes());

            HTTPResponse resp = urlfetch.fetch(httpRequest);
            if (resp.getResponseCode() == 200) {
                String registryKey = getRegistryKey(resp);
                if (!isEmpty(registryKey)) {
                    Entity e = new Entity(createKey("registration", url.getHost()));
                    e.setProperty("REGISTRY_SERVICE_KEY", registryKey);
                    inTransaction(datastore, new PutEntityTransactionCallback(e));
                    registerMappings(serviceUri, configuration);
                }
            }
        }
    }

    private void registerMappings(String serviceUri, JSONObject configuration) throws Exception {
        if (configuration.has("mappings")) {
            JSONArray mappings = configuration.getJSONArray("mappings");
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                mapping.put("service", serviceUri);

                String payload = "mapping=" + mapping.toString();

                String uri = mapping.getString("uri");
                String registerUri = "/addama/registry/mappings" + uri;
                if (uri.startsWith("/addama")) {
                    registerUri = replace(mapping.getString("uri"), "/addama", "/addama/registry/mappings");
                }

                HTTPRequest req = new HTTPRequest(new URL(registryDomain + registerUri), POST);
                req.setHeader(new HTTPHeader("x-addama-apikey", apiKey));
                req.setPayload(payload.getBytes());

                HTTPResponse resp = urlfetch.fetch(req);
                if (resp.getResponseCode() != 200) {
                    log.warning("registerMappings(" + registryDomain + "): failed to register:" + registerUri);
                }
            }
        }
    }

    private String getRegistryKey(HTTPResponse resp) throws Exception {
        for (HTTPHeader header : resp.getHeaders()) {
            if (equalsIgnoreCase(header.getName(), "x-addama-registry-key")) {
                return header.getValue();
            }
        }

        return null;
    }
}

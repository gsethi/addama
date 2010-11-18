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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.urlfetch.*;
import com.google.apphosting.api.ApiProxy;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ServiceRegistrationJsonConfigHandler implements JsonConfigHandler {
    private static final Logger log = Logger.getLogger(ServiceRegistrationJsonConfigHandler.class.getName());
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";

    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
    private final DatastoreServiceTemplate datastoreServiceTemplate = new DatastoreServiceTemplate();

    private final String registryDomain;
    private final String apiKey;

    public ServiceRegistrationJsonConfigHandler(String registryDomain, String apiKey) {
        this.registryDomain = registryDomain;
        this.apiKey = apiKey;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("service")) {
            JSONObject service = configuration.getJSONObject("service");
            service.put("url", "https://" + APPSPOT_HOST);

            String serviceUri = service.getString("uri");
            String registerUri = StringUtils.replace(serviceUri, "/addama/services", "/addama/registry/services");            
            String payload = "service=" + service.toString();

            URL url = new URL(registryDomain + registerUri);
            HTTPRequest httpRequest = new HTTPRequest(url, HTTPMethod.POST);
            httpRequest.setHeader(new HTTPHeader("x-addama-apikey", apiKey));
            httpRequest.setPayload(payload.getBytes());

            HTTPResponse resp = urlFetchService.fetch(httpRequest);
            if (resp.getResponseCode() == 200) {
                String registryKey = getRegistryKey(resp);
                if (!StringUtils.isEmpty(registryKey)) {
                    Entity e = new Entity(KeyFactory.createKey("registration", url.getHost()));
                    e.setProperty("REGISTRY_SERVICE_KEY", registryKey);
                    datastoreServiceTemplate.inTransaction(new PutEntityTransactionCallback(e));
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
                    registerUri = StringUtils.replace(mapping.getString("uri"), "/addama", "/addama/registry/mappings");
                }

                HTTPRequest req = new HTTPRequest(new URL(registryDomain + registerUri), HTTPMethod.POST);
                req.setHeader(new HTTPHeader("x-addama-apikey", apiKey));
                req.setPayload(payload.getBytes());

                HTTPResponse resp = urlFetchService.fetch(req);
                if (resp.getResponseCode() != 200) {
                    log.warning("registerMappings(" + registryDomain + "): failed to register:" + registerUri);
                }
            }
        }
    }

    private String getRegistryKey(HTTPResponse resp) throws Exception {
        for (HTTPHeader header : resp.getHeaders()) {
            if (StringUtils.equalsIgnoreCase(header.getName(), "x-addama-registry-key")) {
                return header.getValue();
            }
        }

        return null;
    }
}

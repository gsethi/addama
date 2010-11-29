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
package org.systemsbiology.addama.coresvcs.gae.filters.callbacks;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.apphosting.api.ApiProxy;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;
import org.systemsbiology.addama.commons.gae.http.MapReduceTooLargeHTTPResponse;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryMapping;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;
import org.systemsbiology.addama.coresvcs.gae.services.Registry;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.HTTPMethod.GET;

/**
 * @author hrovira
 */
public class StaticContentMemcacheLoaderCallback implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(StaticContentMemcacheLoaderCallback.class.getName());
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";

    private final MapReduceTooLargeHTTPResponse mapReduce = new MapReduceTooLargeHTTPResponse();

    private final Registry registry;

    public StaticContentMemcacheLoaderCallback(Registry registry) {
        this.registry = registry;
    }

    public Serializable getCacheableObject(String requestUri) throws Exception {
        log.info("getCacheableObject(" + requestUri + ")");

        RegistryMapping registryMapping = registry.getStaticContentRegistryMapping(requestUri);
        if (registryMapping == null) {
            log.info("getCacheableObject(" + requestUri + "): no mapping found");
            return null;
        }

        RegistryService service = registry.getRegistryService(registryMapping.getServiceUri());
        if (service == null) {
            log.info("getCacheableObject(" + requestUri + "): no service found");
            return null;
        }

        HTTPRequest proxyRequest = new HTTPRequest(new URL(service.getUrl().toString() + requestUri), GET);
        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-key", service.getAccessKey().toString()));
        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_HOST));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPResponse resp = mapReduce.fetch(proxyRequest, outputStream);
        if (resp != null) {
            if (resp.getResponseCode() == HttpServletResponse.SC_OK) {
                return resp.getContent();
            }
        } else {
            return outputStream.toByteArray();
        }
        return null;
    }
}

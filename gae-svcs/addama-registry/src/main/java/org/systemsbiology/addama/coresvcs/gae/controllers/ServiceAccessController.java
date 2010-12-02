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
package org.systemsbiology.addama.coresvcs.gae.controllers;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.coresvcs.gae.pojos.CachedUrl;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;
import org.systemsbiology.addama.coresvcs.gae.services.Proxy;
import org.systemsbiology.addama.coresvcs.gae.services.Registry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ServiceAccessController extends AbstractController {
    private static final Logger log = Logger.getLogger(ServiceAccessController.class.getName());

    private MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(getClass().getName());

    private Registry registry;
    private Proxy proxy;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.fine("handleRequestInternal(" + requestUri + ")");

        CachedUrl cachedUrl = (CachedUrl) memcacheService.get(requestUri);
        if (cachedUrl == null) {
            RegistryService service = registry.getMatchingService(requestUri);
            if (service == null) {
                throw new ResourceNotFoundException(requestUri);
            }

            String serviceUri = StringUtils.substringAfter(requestUri, service.getUri());
            URL targetUrl = new URL(service.getUrl().toString() + serviceUri);
            cachedUrl = new CachedUrl(service, targetUrl);

            log.info("caching url:" + requestUri + ":" + targetUrl);
            memcacheService.put(requestUri, cachedUrl, Expiration.byDeltaSeconds(60));
        }

        proxy.doAction(request, response, cachedUrl.getTargetUrl(), cachedUrl.getAccessKey());
        return null;
    }
}
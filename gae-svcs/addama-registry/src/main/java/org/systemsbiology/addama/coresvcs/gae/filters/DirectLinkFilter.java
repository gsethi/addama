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
package org.systemsbiology.addama.coresvcs.gae.filters;

import com.google.appengine.api.urlfetch.*;
import com.google.apphosting.api.ApiProxy;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryMapping;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;
import org.systemsbiology.addama.coresvcs.gae.services.Registry;
import org.systemsbiology.addama.coresvcs.gae.services.Sharing;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.doNotFollowRedirects;
import static com.google.appengine.api.urlfetch.HTTPMethod.POST;

/**
 * @author hrovira
 */
public class DirectLinkFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(DirectLinkFilter.class.getName());
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";

    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    private Registry registry;
    private Sharing sharing;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setSharing(Sharing sharing) {
        this.sharing = sharing;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String requestUri = request.getRequestURI();
        if (requestUri.endsWith("/directlink")) {
            String directLinkUri = StringUtils.substringBeforeLast(requestUri, "/directlink");
            log.info("doFilter(" + directLinkUri + "): directlink");

            HttpServletResponse response = (HttpServletResponse) servletResponse;

            RegistryService registryService = getRegistryService(directLinkUri);
            if (registryService == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                sharing.checkAccess(registryService, request);
            } catch (ForbiddenAccessException e) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            URL url = new URL(registryService.getUrl().toString() + directLinkUri);
            UUID accessKey = registryService.getAccessKey();

            HTTPRequest req = new HTTPRequest(new URL(url.toString() + "/client_redirect"), POST, doNotFollowRedirects());
            req.setHeader(new HTTPHeader("x-addama-registry-key", accessKey.toString()));
            req.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_HOST));
            req.setHeader(new HTTPHeader("x-addama-registry-client", request.getRemoteAddr()));

            HTTPResponse resp = urlFetchService.fetch(req);
            if (resp.getResponseCode() == 302) {
                String location = getLocation(resp);
                if (!StringUtils.isEmpty(location)) {
                    response.setContentType("application/json");
                    response.getWriter().write("{location:\"" + location + "\"}");
                    return;
                }
            }
        }

        log.info("doFilter(" + requestUri + "): not directlink request");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /*
    * Private Methods
    */

    private RegistryService getRegistryService(String requestUri) {
        RegistryMapping mapping = registry.getRegistryMapping(requestUri);
        if (mapping == null) {
            return null;
        }

        String serviceUri = mapping.getServiceUri();
        RegistryService registryService = registry.getRegistryService(serviceUri);
        if (registryService == null) {
            return null;
        }
        return registryService;
    }

    private String getLocation(HTTPResponse response) {
        for (HTTPHeader header : response.getHeaders()) {
            if (StringUtils.equalsIgnoreCase(header.getName(), "Location")) {
                return header.getValue();
            }
        }
        return null;
    }
}

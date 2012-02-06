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
package org.systemsbiology.addama.appengine.filters;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.apache.commons.lang.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.systemsbiology.addama.commons.web.servlet.HttpRequestUriInvocationHandler.instrumentRequest;

/**
 * @author hrovira
 */
public class CheckRegistryServiceKeyFilter implements Filter {
    private static final Logger log = Logger.getLogger(CheckRegistryServiceKeyFilter.class.getName());

    private final MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(getClass().getName());
    private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    /*
     * Filter
     */

    public void init(FilterConfig filterConfig) throws ServletException {
        log.fine("init");
    }

    public void destroy() {
        log.fine("destroy");
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (isDevMode()) {
            log.info("DEVMODE");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {
            if (StringUtils.equals(request.getRequestURI(), "/registration")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            String requestUri = request.getRequestURI();
            String singleCall = request.getContextPath() + request.getServletPath() + "/singlecall/";

            String regKey = getRegistryServiceKey(request);
            if (!StringUtils.isEmpty(regKey)) {
                String key = request.getHeader("x-addama-registry-key");
                if (StringUtils.equals(key, regKey)) {
                    if (requestUri.endsWith("/client_redirect") && StringUtils.equalsIgnoreCase(request.getMethod(), "post")) {
                        String actualUri = StringUtils.substringBeforeLast(requestUri, "/client_redirect");
                        String fileUri = StringUtils.substringAfterLast(actualUri, "/");
                        String temporaryUri = singleCall + UUID.randomUUID().toString() + "/" + fileUri;
                        log.fine("adding single call: " + temporaryUri + ":" + actualUri);
                        memcacheService.put(temporaryUri, actualUri);

                        response.sendRedirect(temporaryUri);
                        return;
                    }

                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }

            if (requestUri.startsWith(singleCall)) {
                log.fine("processing single call");
                String temporaryUri = request.getRequestURI();
                if (memcacheService.contains(temporaryUri)) {
                    String actualUri = (String) memcacheService.get(temporaryUri);
                    log.fine("removing single call from memcache: " + actualUri);
                    memcacheService.delete(temporaryUri);

                    // TODO : Handle passing registry user
                    HttpServletRequest instrumented = instrumentRequest(request, actualUri, null);
                    filterChain.doFilter(instrumented, servletResponse);
                    return;
                }
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            log.warning("doFilter: " + e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    /*
     * Private Methods
     */

    private String getRegistryServiceKey(HttpServletRequest request) {
        String domain = request.getHeader("x-addama-registry-host");
        log.fine("getRegistryServiceKey(" + request.getRequestURI() + "):registry.domain=" + domain);
        try {
            Entity e = datastoreService.get(KeyFactory.createKey("registration", domain));
            return e.getProperty("REGISTRY_SERVICE_KEY").toString();
        } catch (Exception e) {
            log.warning("getRegistryServiceKey(): key not found for " + domain + ": " + e);
        }
        return null;
    }

    private boolean isDevMode() {
        try {
            String devMode = System.getProperty("gae.addama.registry.dev.mode");
            if (!StringUtils.isEmpty(devMode)) {
                return Boolean.parseBoolean(devMode);
            }
        } catch (Exception e) {
            log.warning("isDevMode:" + e);
        }
        return false;
    }
}
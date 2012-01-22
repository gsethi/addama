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

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.appengine.pojos.CachedUrl;
import org.systemsbiology.addama.appengine.pojos.RegistryMapping;
import org.systemsbiology.addama.appengine.pojos.RegistryService;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.systemsbiology.addama.appengine.util.Proxy.*;
import static org.systemsbiology.addama.appengine.util.Registry.getMatchingRegistryMappings;
import static org.systemsbiology.addama.appengine.util.Registry.getRegistryService;
import static org.systemsbiology.addama.appengine.util.Sharing.checkAccess;

/**
 * @author hrovira
 */
public class ProxiesFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(ProxiesFilter.class.getName());

    private final URLFetchService urlFetchService = getURLFetchService();
    private final MemcacheService memcachedUrls = getMemcacheService(getClass().getName() + ".cachedurls");

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = request.getRequestURI();
        if (shouldSkip(requestUri)) {
            log.fine("doFilter(" + requestUri + "): skipping addama registry URIs");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        CachedUrl[] cachedUrls = getCachedUrls(requestUri);
        if (cachedUrls == null || cachedUrls.length == 0) {
            log.warning("doFilter(" + requestUri + "): no mappings found in the registry");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (equalsIgnoreCase(request.getMethod(), "get") && cachedUrls.length > 1) {
            doGetMultiAction(request, response, cachedUrls);
        } else {
            CachedUrl cachedUrl = cachedUrls[0];
            try {
                checkAccess(cachedUrl.getRegistryService(), request);
                doAction(request, response, cachedUrl.getTargetUrl(), cachedUrl.getAccessKey());
            } catch (ForbiddenAccessException e) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    /*
    * Private Methods
    */

    private boolean shouldSkip(String requestUri) {
        if (requestUri.equals("/addama")) return true;
        if (requestUri.equals("/addama/")) return true;
        if (requestUri.equals("/addama/repositories")) return true;
        if (requestUri.equals("/addama/datasources")) return true;
        if (requestUri.equals("/addama/chromosomes")) return true;
        if (requestUri.equals("/addama/workspaces")) return true;
        if (requestUri.equals("/addama/indexes")) return true;
        if (requestUri.equals("/addama/services")) return true;
        if (requestUri.equals("/addama/apps")) return true;
        if (requestUri.equals("/addama/channels")) return true;

        if (requestUri.startsWith("/addama/apps")) return true;
        if (requestUri.startsWith("/addama/channels")) return true;
        if (requestUri.startsWith("/addama/registry")) return true;
        if (requestUri.startsWith("/addama/asynch")) return true;
        if (requestUri.startsWith("/addama/search")) return true;
        if (requestUri.startsWith("/addama/groups")) return true;
        if (requestUri.startsWith("/addama/users")) return true;
        if (requestUri.startsWith("/addama/apikeys")) return true;
        if (requestUri.startsWith("/addama/memcache")) return true;

        return false;
    }

    private CachedUrl[] getCachedUrls(String requestUri) throws MalformedURLException {
        CachedUrl[] cachedUrls = (CachedUrl[]) memcachedUrls.get(requestUri);
        if (cachedUrls != null && cachedUrls.length > 0) {
            return cachedUrls;
        }

        Iterable<RegistryMapping> mappings = getMatchingRegistryMappings(requestUri);
        if (mappings == null) {
            log.warning("no mappings found in the registry for: " + requestUri);
            return null;
        }

        ArrayList<CachedUrl> cachedUrlList = new ArrayList<CachedUrl>();

        for (RegistryMapping mapping : mappings) {
            String serviceUri = mapping.getServiceUri();
            RegistryService registryService = getRegistryService(serviceUri);
            if (registryService == null) {
                log.warning("getCachedUrls(" + requestUri + "): service not found for mapping: " + serviceUri);
            } else {
                URL targetUrl = new URL(registryService.getUrl().toString() + requestUri);
                cachedUrlList.add(new CachedUrl(registryService, targetUrl));
            }
        }

        CachedUrl[] newCachedUrls = cachedUrlList.toArray(new CachedUrl[cachedUrlList.size()]);
        memcachedUrls.put(requestUri, newCachedUrls, Expiration.byDeltaSeconds(60));
        return newCachedUrls;
    }

    private void doGetMultiAction(HttpServletRequest request, HttpServletResponse response, CachedUrl[] cachedUrls) throws IOException {
        try {
            ArrayList<Future<HTTPResponse>> futureResponses = new ArrayList<Future<HTTPResponse>>();
            for (CachedUrl cachedUrl : cachedUrls) {
                RegistryService registryService = cachedUrl.getRegistryService();
                try {
                    checkAccess(registryService, request);

                    HTTPRequest futureReq = getWithParams(request, cachedUrl.getTargetUrl());
                    forwardHeaders(request, futureReq, cachedUrl.getAccessKey());
                    futureResponses.add(urlFetchService.fetchAsync(futureReq));
                } catch (ForbiddenAccessException e) {
                    log.warning("access to service denied:" + registryService.getUri());
                }
            }

            JSONObject json = new JSONObject();
            for (Future<HTTPResponse> futureResponse : futureResponses) {
                try {
                    accumulate(json, futureResponse);
                } catch (Exception e) {
                    log.warning("doGetMultiAction(" + request.getRequestURI() + "):" + e);
                }
            }

            if (json.has("items")) {
                json.put("numberOfItems", json.getJSONArray("items").length());
            }

            response.setContentType("application/json");
            response.getWriter().write(json.toString());
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    private void accumulate(JSONObject json, Future<HTTPResponse> futureResponse)
            throws ExecutionException, InterruptedException, JSONException {
        HTTPResponse resp = futureResponse.get();
        log.info("responseCode=" + resp.getResponseCode());

        String content = new String(resp.getContent());
        JSONObject jsonResponse = new JSONObject(content);

        Iterator itr = jsonResponse.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            JSONArray values = jsonResponse.optJSONArray(key);
            if (values != null) {
                for (int i = 0; i < values.length(); i++) {
                    json.append(key, values.get(i));
                }
            } else {
                json.accumulate(key, jsonResponse.get(key));
            }
        }
    }
}

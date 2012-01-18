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
package org.systemsbiology.addama.registry;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.OkJsonResponseCallback;
import org.systemsbiology.addama.commons.spring.PropertiesFileLoader;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.registry.brokering.MaxContentLengthHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.servlet.HttpRequestUriInvocationHandler.instrumentRequest;

/**
 * @author hrovira
 */
public class RegistryServiceFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(RegistryServiceFilter.class.getName());
    public static final Integer MAX_CONTENT_LENGTH = 10000000;

    protected final Map<String, String> registryServiceKeyByHost = new HashMap<String, String>();

    private transient HashMap<String, String> temporaryUris = new HashMap<String, String>();

    private HttpClientTemplate httpClientTemplate;
    private PropertiesFileLoader propertiesFileLoader;
    private ServiceConfig serviceConfig;

    private boolean runUnregistered = false;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setPropertiesFileLoader(PropertiesFileLoader propertiesFileLoader) {
        this.propertiesFileLoader = propertiesFileLoader;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    /*
    * GenericFilterBean
    */
    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();

        String contextPath = super.getServletContext().getContextPath();
        if (contextPath.startsWith("/")) {
            contextPath = substringAfter(contextPath, "/");
        }

        if (!propertiesFileLoader.loaded()) {
            log.warning("SERVICE [" + contextPath + "] will run in STAND-ALONE mode :: no addama.properties found in classpath");
            this.runUnregistered = true;
            return;
        }

        String hostUrl = propertiesFileLoader.getProperty("httpclient.secureHostUrl");
        if (isEmpty(hostUrl)) {
            throw new ServletException("SERVICE [" + contextPath + "] registry URL not configured in 'addama.properties' [httpclient.secureHostUrl]");
        }

        String serviceUrl = propertiesFileLoader.getProperty("service.hostUrl");
        if (isEmpty(serviceUrl)) {
            throw new ServletException("SERVICE [" + contextPath + "] service host not configured  in 'addama.properties' [service.hostUrl]");
        }

        try {
            URL secureHostUrl = new URL(hostUrl);
            URL serviceHostUrl = new URL(chomp(serviceUrl, "/") + "/" + contextPath);

            JSONObject registration = new JSONObject();
            registration.put("url", serviceHostUrl.toString());
            registration.put("label", serviceConfig.LABEL());
            registration.put("family", serviceConfig.FAMILY());
            registration.put("searchable", serviceConfig.JSON().optBoolean("searchable", false));

            for (Mapping m : serviceConfig.getMappings()) {
                JSONObject mapping = new JSONObject();
                mapping.put("id", m.ID());
                mapping.put("label", m.LABEL());
                if (m.JSON().has("family")) {
                    mapping.put("family", chomp(m.JSON().getString("family"), "/"));
                }
                if (m.JSON().has("pattern")) {
                    mapping.put("pattern", chomp(m.JSON().getString("pattern"), "/"));
                }
                registration.append("mappings", mapping);
            }

            PostMethod post = new PostMethod("/addama/registry");
            post.setQueryString(new NameValuePair[]{new NameValuePair("registration", registration.toString())});

            String registryKey = (String) httpClientTemplate.executeMethod(post, new RegistrationCallback(serviceConfig));
            if (!isEmpty(registryKey)) {
                registryServiceKeyByHost.put(secureHostUrl.getHost(), registryKey);
                broadcastToAdmins("Registration Completed for " + serviceConfig.LABEL());
            } else {
                broadcastToAdmins("Registration Issues for " + serviceConfig.LABEL() + ": Check Logs");
            }
        } catch (Exception e) {
            broadcastToAdmins("Registration Issues for " + serviceConfig.LABEL() + ":" + e.getMessage());
            throw new ServletException("SERVICE [" + contextPath + "]", e);
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (this.runUnregistered) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = request.getRequestURI();
        log.fine(requestUri);

        String singleCall = request.getContextPath() + request.getServletPath() + "/singlecall/";
        if (requestUri.startsWith(singleCall)) {
            log.fine("single call:" + singleCall);
            if (temporaryUris.containsKey(requestUri)) {
                String actualUri = temporaryUris.remove(requestUri);
                log.fine("processing single call: " + actualUri);
                HttpServletRequest instrumented = instrumentRequest(request, actualUri);
                filterChain.doFilter(instrumented, servletResponse);
                return;
            }
        }

        if (!hasRegistryKey(request)) {
            log.warning("does not have registry key: " + requestUri);
            response.setStatus(SC_UNAUTHORIZED);
            return;
        }

        if (equalsIgnoreCase(request.getMethod(), "post") && requestUri.endsWith("/client_redirect")) {
            String actualUri = substringBeforeLast(requestUri, "/client_redirect");

            log.fine("client_redirect:" + actualUri);
            response.sendRedirect(getSingleCallTemporaryUri(request, singleCall, actualUri));
            return;
        }

        if (equalsIgnoreCase(request.getMethod(), "get")) {
            if (contains(requestUri, "/_afdl/")) {
                response.setStatus(SC_REQUEST_ENTITY_TOO_LARGE);
                response.setHeader("Location", getSingleCallTemporaryUri(request, singleCall, requestUri));
                return;
            }

            MaxContentLengthHttpServletResponse max = new MaxContentLengthHttpServletResponse(response, MAX_CONTENT_LENGTH);
            filterChain.doFilter(servletRequest, max);
            if (max.isLimitExceeded()) {
                log.fine("size limit exceeded:" + MAX_CONTENT_LENGTH);

                String location = getSingleCallTemporaryUri(request, singleCall, requestUri);
                response.setStatus(SC_REQUEST_ENTITY_TOO_LARGE);
                response.setHeader("Location", location);
                return;
            }
        }

        log.fine("serve normal");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /*
    * Private Methods
    */

    private boolean hasRegistryKey(HttpServletRequest request) {
        String key = request.getHeader("x-addama-registry-key");
        if (isEmpty(key)) {
            return false;
        }

        String host = request.getHeader("x-addama-registry-host");
        if (isEmpty(host)) {
            return false;
        }

        String savedKey = registryServiceKeyByHost.get(host);
        return !isEmpty(savedKey) && equalsIgnoreCase(key, savedKey);
    }

    private String getSingleCallTemporaryUri(HttpServletRequest request, String singleCall, String actualUri) {
        String fileUri = substringAfterLast(actualUri, "/");
        String temporaryUri = singleCall + UUID.randomUUID().toString() + "/" + fileUri;
        temporaryUris.put(temporaryUri, actualUri);

        return chomp(substringBefore(request.getRequestURL().toString(), actualUri), "/") + temporaryUri;
    }

    private void broadcastToAdmins(String message) {
        try {
            JSONObject event = new JSONObject().put("title", "Service Registration").put("message", message);
            PostMethod post = new PostMethod("/addama/channels/admins");
            post.setQueryString(new NameValuePair[]{new NameValuePair("event", event.toString())});
            httpClientTemplate.executeMethod(post, new OkJsonResponseCallback());
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }
}
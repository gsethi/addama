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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;
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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.servlet.HttpRequestUriInvocationHandler.instrumentRequest;

/**
 * @author hrovira
 */
public class RegistryServiceFilter extends GenericFilterBean implements ResponseCallback {
    private static final Logger log = Logger.getLogger(RegistryServiceFilter.class.getName());
    public static final Integer MAX_CONTENT_LENGTH = 10000000;

    protected final Map<String, String> registryServiceKeyByHost = new HashMap<String, String>();

    private transient HashMap<String, String> temporaryUris = new HashMap<String, String>();

    private HttpClientTemplate httpClientTemplate;
    private PropertiesFileLoader propertiesFileLoader;
    private URL secureHostUrl;
    private boolean runUnregistered = false;

    private ServiceConfig serviceConfig;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setPropertiesFileLoader(PropertiesFileLoader propertiesFileLoader) {
        this.propertiesFileLoader = propertiesFileLoader;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();

        if (!propertiesFileLoader.loaded()) {
            log.warning("service will run in STAND-ALONE mode :: no addama.properties found in classpath");
            this.runUnregistered = true;
            return;
        }

        String hostUrl = propertiesFileLoader.getProperty("httpclient.secureHostUrl");
        if (isEmpty(hostUrl)) {
            throw new ServletException("registry URL not configured in 'addama.properties' [httpclient.secureHostUrl]");
        }

        String serviceUrl = propertiesFileLoader.getProperty("service.hostUrl");
        if (isEmpty(serviceUrl)) {
            throw new ServletException("service host not configured  in 'addama.properties' [service.hostUrl]");
        }

        try {
            this.secureHostUrl = new URL(hostUrl);
            URL serviceHostUrl = new URL(chomp(serviceUrl) + "/" + super.getServletContext().getContextPath());

            JSONObject registration = new JSONObject();
            registration.put("url", serviceHostUrl.toString());
            registration.put("label", serviceConfig.LABEL());
            registration.put("searchable", serviceConfig.JSON().optBoolean("searchable", false));

            for (Mapping m : serviceConfig.getMappings()) {
                JSONObject mapping = new JSONObject();
                mapping.put("uri", m.URI());
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
            httpClientTemplate.executeMethod(post, this);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /*
     * ResponseCallback
     */
    public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
        String message = null;
        try {
            if (statusCode == 200) {
                Header header = method.getResponseHeader("x-addama-registry-key");
                if (header != null) {
                    registryServiceKeyByHost.put(secureHostUrl.getHost(), header.getValue());
                }
            }
            message = method.getResponseBodyAsString();
        } catch (Exception e) {
            log.warning(e.getMessage());
        } finally {
            StringBuilder builder = new StringBuilder();
            builder.append("\n===============================================\n");
            builder.append("Service Registration");
            builder.append("\n\t").append(serviceConfig.LABEL());
            if (statusCode == SC_OK) {
                builder.append(" [ OK ]");
            } else {
                builder.append(" [ ").append(statusCode).append(" ]");
            }
            if (!isEmpty(message)) {
                builder.append("\n\t").append(message);
            }

            for (Mapping mapping : serviceConfig.getMappings()) {
                builder.append("\n\t").append(mapping.LABEL());
            }
            builder.append("\n===============================================\n");
            log.info(builder.toString());
        }

        return null;
    }

    /*
    * GenericFilterBean
    */

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

}
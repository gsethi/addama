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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientException;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;
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
public class JsonConfigRegistryServiceFilter extends GenericFilterBean implements JsonConfigHandler {
    private static final Logger log = Logger.getLogger(JsonConfigRegistryServiceFilter.class.getName());

    protected final Map<String, String> registryServiceKeyByHost = new HashMap<String, String>();

    private transient HashMap<String, String> temporaryUris = new HashMap<String, String>();

    private HttpClientTemplate httpClientTemplate;
    private URL secureHostUrl;

    private final Integer maxContentLength = 10000000;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(this);
    }

    public void setSecureHostUrl(URL secureHostUrl) {
        this.secureHostUrl = secureHostUrl;
    }

    /*
    * GenericFilterBean
    */

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
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
            
            MaxContentLengthHttpServletResponse max = new MaxContentLengthHttpServletResponse(response, maxContentLength);
            filterChain.doFilter(servletRequest, max);
            if (max.isLimitExceeded()) {
                log.fine("size limit exceeded:" + maxContentLength);

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
    * JsonConfigHandler
    */

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("service")) {
            JSONObject service = configuration.getJSONObject("service");
            String serviceUri = service.getString("uri");
            httpClientTemplate.setServiceUri(serviceUri);

            String registerUri = replace(serviceUri, "/addama/services", "/addama/registry/services");
            PostMethod post = new PostMethod(registerUri);
            post.setQueryString(new NameValuePair[]{new NameValuePair("service", service.toString())});

            String key = (String) httpClientTemplate.executeMethod(post, new RegistrationResponseCallback(service));
            if (!isEmpty(key)) {
                registryServiceKeyByHost.put(secureHostUrl.getHost(), key);

                if (configuration.has("mappings")) {
                    JSONArray mappings = configuration.getJSONArray("mappings");
                    for (int i = 0; i < mappings.length(); i++) {
                        JSONObject mapping = mappings.getJSONObject(i);
                        mapping.put("service", serviceUri);
                        registerMapping(mapping);
                    }
                }
            }

            registrationReport(service, configuration.optJSONArray("mappings"));
        }
    }

    /*
    * Private Methods
    */

    private void registerMapping(JSONObject mapping) throws JSONException, HttpClientException, HttpClientResponseException {
        String uri = mapping.getString("uri");
        String registerUri = "/addama/registry/mappings" + uri;
        if (uri.startsWith("/addama")) {
            registerUri = replace(uri, "/addama", "/addama/registry/mappings");
        }

        PostMethod method = new PostMethod(registerUri);
        method.addParameter(new NameValuePair("mapping", mapping.toString()));
        httpClientTemplate.executeMethod(method, new RegistrationResponseCallback(mapping));
    }

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
        if (isEmpty(savedKey)) {
            return false;
        }

        return equalsIgnoreCase(key, savedKey);
    }

    private String getSingleCallTemporaryUri(HttpServletRequest request, String singleCall, String actualUri) {
        String fileUri = substringAfterLast(actualUri, "/");
        String temporaryUri = singleCall + UUID.randomUUID().toString() + "/" + fileUri;
        temporaryUris.put(temporaryUri, actualUri);

        return chomp(substringBefore(request.getRequestURL().toString(), actualUri), "/") + temporaryUri;
    }

    private void registrationReport(JSONObject service, JSONArray mappings) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n===============================================\n");
            builder.append("Registering Service");
            appendRegistrationReport(builder, service);

            if (mappings != null) {
                builder.append("\n\nRegistering ").append(mappings.length()).append(" Mappings");
                for (int i = 0; i < mappings.length(); i++) {
                    appendRegistrationReport(builder, mappings.getJSONObject(i));
                }
            }

            builder.append("\n===============================================\n");
            log.info(builder.toString());
        } catch (JSONException e) {
            log.warning("unable to print registration report:" + e);
        }
    }

    private void appendRegistrationReport(StringBuilder builder, JSONObject item) throws JSONException {
        String uri = item.getString("uri");
        String label = item.optString("label", uri);
        Integer responseCode = item.optInt("responseCode", 404);

        if (responseCode == SC_OK) {
            builder.append("\n\t").append(label).append(" [ OK ]");
        } else {
            builder.append("\n\t").append(label).append(" [ ").append(responseCode).append(" ]");
        }
    }

    /*
    * Private Classes
    */

    private class RegistrationResponseCallback implements ResponseCallback {
        private final JSONObject item;

        private RegistrationResponseCallback(JSONObject item) {
            this.item = item;
        }

        public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
            try {
                item.put("responseCode", statusCode);
            } catch (JSONException e) {
                throw new HttpClientResponseException(statusCode, method, e);
            }

            if (statusCode == 200) {
                Header header = method.getResponseHeader("x-addama-registry-key");
                if (header != null) {
                    return header.getValue();
                }
            }
            return null;
        }
    }
}
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
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.commons.httpclient.support.*;

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

/**
 * @author hrovira
 */
public class JsonConfigRegistryServiceFilter extends GenericFilterBean implements JsonConfigHandler, ResponseCallback {
    private static final Logger log = Logger.getLogger(JsonConfigRegistryServiceFilter.class.getName());

    private final Map<String, String> registryServiceKeyByHost = new HashMap<String, String>();

    private transient HashMap<String, String> temporaryUris = new HashMap<String, String>();

    private HttpClientTemplate httpClientTemplate;
    private JsonConfig jsonConfig;
    private URL secureHostUrl;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void setSecureHostUrl(URL secureHostUrl) {
        this.secureHostUrl = secureHostUrl;
    }

    /*
    * GenericFilterBean
    */

    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();
        try {
            this.jsonConfig.processConfiguration(this);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = request.getRequestURI();
        String singleCall = request.getContextPath() + request.getServletPath() + "/singlecall/";
        if (hasRegistryKey(request)) {
            if (requestUri.endsWith("/client_redirect") && StringUtils.equalsIgnoreCase(request.getMethod(), "post")) {
                String actualUri = StringUtils.substringBeforeLast(requestUri, "/client_redirect");
                String fileUri = StringUtils.substringAfterLast(actualUri, "/");
                String temporaryUri = singleCall + UUID.randomUUID().toString() + "/" + fileUri;
                temporaryUris.put(temporaryUri, actualUri);

                log.fine("redirecting to " + temporaryUri);
                response.sendRedirect(temporaryUri);
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (requestUri.startsWith(singleCall)) {
            log.fine("checking single call" + requestUri);
            if (temporaryUris.containsKey(requestUri)) {
                String actualUri = temporaryUris.remove(requestUri);
                log.fine("processing single call: " + actualUri);
                filterChain.doFilter(new MockHttpServletReq(request, actualUri), servletResponse);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /*
    * JsonConfigHandler
    */

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("service")) {
            JSONObject service = configuration.getJSONObject("service");
            String serviceUri = service.getString("uri");
            httpClientTemplate.setServiceUri(serviceUri);

            String registerUri = StringUtils.replace(serviceUri, "/addama/services", "/addama/registry/services");
            PostMethod post = new PostMethod(registerUri);
            post.setQueryString(new NameValuePair[]{new NameValuePair("service", service.toString())});
            String key = (String) httpClientTemplate.executeMethod(post, this);
            if (StringUtils.isEmpty(key)) {
                log.warning("failed to register service:" + registerUri);
                return;
            }

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
    }

    /*
    * ResponseCallback
    */

    public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
        if (statusCode == 200) {
            Header header = method.getResponseHeader("x-addama-registry-key");
            if (header != null) {
                return header.getValue();
            }
        }
        return null;
    }


    /*
    * Private Methods
    */

    private void registerMapping(JSONObject mapping) throws JSONException, HttpClientException, HttpClientResponseException {
        String uri = mapping.getString("uri");
        String registerUri = "/addama/registry/mappings" + uri;
        if (uri.startsWith("/addama")) {
            registerUri = StringUtils.replace(uri, "/addama", "/addama/registry/mappings");
        }

        PostMethod method = new PostMethod(registerUri);
        method.addParameter(new NameValuePair("mapping", mapping.toString()));
        int statusCode = (Integer) httpClientTemplate.executeMethod(method, new StatusCodeCaptureResponseCallback());
        if (statusCode != 200) {
            log.warning("failed to register mapping:" + registerUri);
        }
    }

    private boolean hasRegistryKey(HttpServletRequest request) {
        String key = request.getHeader("x-addama-registry-key");
        if (StringUtils.isEmpty(key)) {
            return false;
        }

        String host = request.getHeader("x-addama-registry-host");
        if (StringUtils.isEmpty(host)) {
            return false;
        }

        String savedKey = registryServiceKeyByHost.get(host);
        if (StringUtils.isEmpty(savedKey)) {
            return false;
        }

        return StringUtils.equals(key, savedKey);
    }
}
package org.systemsbiology.addama.sandbox.httpproxy;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class SimpleProxyController implements InitializingBean {
    private static final Logger log = Logger.getLogger(SimpleProxyController.class.getName());

    private final Map<String, String> proxyMappings = new HashMap<String, String>();
    private final Map<String, Boolean> excludeBaseUris = new HashMap<String, Boolean>();
    private final Map<String, String> defaultPages = new HashMap<String, String>();

    private HttpClientTemplate httpClientTemplate;
    private JsonConfig jsonConfig;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    /*
     * InitializingBean
     */

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new JsonConfigHandler() {
            public void handle(JSONObject configuration) throws Exception {
                if (configuration.has("locals")) {
                    JSONArray locals = configuration.getJSONArray("locals");
                    for (int i = 0; i < locals.length(); i++) {
                        JSONObject local = locals.getJSONObject(i);
                        String uri = local.getString("uri");
                        proxyMappings.put(uri, local.getString("proxy"));
                        if (local.has("excludeBaseUri")) {
                            excludeBaseUris.put(uri, local.getBoolean("excludeBaseUri"));
                        }
                        if (local.has("defaultPage")) {
                            defaultPages.put(uri, local.getString("defaultPage"));
                        }
                    }
                }
            }
        });
    }

    @RequestMapping
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("proxy(" + request.getRequestURI() + ")");

        HttpMethodBase method = getProxyMethod(request);
        method.setFollowRedirects(false);

        forwardParameters(request, method);
        forwardHeaders(request, method);
        httpClientTemplate.executeMethod(method, new SimpleProxyResponseCallback(response, getDefaultPage(request)));
    }

    /*
     * Private Methods
     */

    private HttpMethodBase getProxyMethod(HttpServletRequest request) throws ResourceNotFoundException {
        String method = request.getMethod();
        String proxyUrl = getProxyUrl(request);
        if (StringUtils.equalsIgnoreCase(method, "GET")) {
            return new GetMethod(proxyUrl);
        }

        if (StringUtils.equalsIgnoreCase(method, "POST")) {
            return new PostMethod(proxyUrl);
        }

        if (StringUtils.equalsIgnoreCase(method, "DELETE")) {
            return new DeleteMethod(proxyUrl);
        }

        throw new UnsupportedOperationException(method + ":" + proxyUrl);

    }

    private String getProxyUrl(HttpServletRequest request) throws ResourceNotFoundException {
        String requestUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
        for (Map.Entry<String, String> entry : proxyMappings.entrySet()) {
            String uri = entry.getKey();
            if (requestUri.startsWith(uri)) {
                if (excludeBaseUri(uri)) {
                    String nonBaseUri = StringUtils.substringAfterLast(requestUri, uri);
                    return entry.getValue() + nonBaseUri;
                }
                return entry.getValue() + requestUri;
            }
        }
        throw new ResourceNotFoundException("no proxy url found for [" + requestUri + "]");
    }

    private String getDefaultPage(HttpServletRequest request) throws ResourceNotFoundException {
        String requestUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
        for (Map.Entry<String, String> entry : defaultPages.entrySet()) {
            String uri = entry.getKey();
            if (requestUri.startsWith(uri)) {
                return defaultPages.get(uri);
            }
        }
        return null;
    }

    private void forwardParameters(HttpServletRequest request, HttpMethodBase method) {
        if (method instanceof PostMethod) {
            PostMethod post = (PostMethod) method;
            Enumeration paramEnum = request.getParameterNames();
            while (paramEnum.hasMoreElements()) {
                String param = (String) paramEnum.nextElement();
                String[] values = request.getParameterValues(param);
                for (String value : values) {
                    post.addParameter(param, value);
                }
            }
            return;
        }

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        Enumeration paramEnum = request.getParameterNames();
        while (paramEnum.hasMoreElements()) {
            String param = (String) paramEnum.nextElement();
            String[] values = request.getParameterValues(param);
            for (String value : values) {
                pairs.add(new NameValuePair(param, value));
            }
        }

        if (!pairs.isEmpty()) {
            method.setQueryString(pairs.toArray(new NameValuePair[pairs.size()]));
        }
    }

    private void forwardHeaders(HttpServletRequest request, HttpMethodBase method) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (!isReservedHeader(headerName)) {
                method.setRequestHeader(headerName, request.getHeader(headerName));
            }
        }
    }

    private boolean isReservedHeader(String headerName) {
        if (StringUtils.equalsIgnoreCase(headerName, "x-addama-apikey")) {
            return true;
        }
        if (StringUtils.equalsIgnoreCase(headerName, "x-addama-registry-key")) {
            return true;
        }
        if (StringUtils.equalsIgnoreCase(headerName, "x-addama-registry-host")) {
            return true;
        }
        if (StringUtils.equalsIgnoreCase(headerName, "x-addama-service-uri")) {
            return true;
        }
        if (StringUtils.equalsIgnoreCase(headerName, "content-length")) {
            return true;
        }
        return false;
    }

    private boolean excludeBaseUri(String uri) {
        if (excludeBaseUris.containsKey(uri)) {
            return excludeBaseUris.get(uri);
        }
        return false;
    }
}

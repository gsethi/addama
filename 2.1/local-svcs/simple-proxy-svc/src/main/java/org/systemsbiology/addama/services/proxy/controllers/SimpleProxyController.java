package org.systemsbiology.addama.services.proxy.controllers;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.BooleanMapJsonConfigHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.addama.services.proxy.callbacks.SimpleProxyResponseCallback;
import org.systemsbiology.addama.services.proxy.transforms.Transforms;
import org.systemsbiology.addama.services.proxy.transforms.TsvToJsonArrayResponseTransform;
import org.systemsbiology.addama.services.proxy.transforms.TsvToJsonItemsResponseTransform;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
@Controller
public class SimpleProxyController {
    private final Map<String, String> proxyMappings = new HashMap<String, String>();
    private final Map<String, Boolean> excludeBaseUris = new HashMap<String, Boolean>();
    private final Map<String, String> defaultPages = new HashMap<String, String>();

    private HttpClientTemplate httpClientTemplate;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new StringMapJsonConfigHandler(proxyMappings, "proxy"));
        jsonConfig.visit(new StringMapJsonConfigHandler(defaultPages, "defaultPage"));
        jsonConfig.visit(new BooleanMapJsonConfigHandler(excludeBaseUris, "excludeBaseUri"));
    }

    @RequestMapping
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpMethodBase method = getProxyMethod(request);
        method.setFollowRedirects(false);

        forwardParameters(request, method);
        forwardHeaders(request, method);

        SimpleProxyResponseCallback callback = new SimpleProxyResponseCallback(response, getDefaultPage(request));

        String transform = request.getHeader("x-addama-simpleproxy-transform");
        if (!isEmpty(transform)) {
            switch (Transforms.valueOf(transform)) {
                case tsvToJsonArray:
                    callback.setResponseTransform(new TsvToJsonArrayResponseTransform());
                    break;
                case tsvToJsonItems:
                    callback.setResponseTransform(new TsvToJsonItemsResponseTransform());
                    break;
            }
        }
        httpClientTemplate.executeMethod(method, callback);
    }

    /*
     * Private Methods
     */

    private HttpMethodBase getProxyMethod(HttpServletRequest request) throws ResourceNotFoundException {
        String method = request.getMethod();
        String proxyUrl = getProxyUrl(request);
        if (equalsIgnoreCase(method, "GET")) {
            return new GetMethod(proxyUrl);
        }

        if (equalsIgnoreCase(method, "POST")) {
            return new PostMethod(proxyUrl);
        }

        if (equalsIgnoreCase(method, "DELETE")) {
            return new DeleteMethod(proxyUrl);
        }

        throw new UnsupportedOperationException(method + ":" + proxyUrl);

    }

    private String getProxyUrl(HttpServletRequest request) throws ResourceNotFoundException {
        String requestUri = substringAfter(request.getRequestURI(), request.getContextPath());
        for (Map.Entry<String, String> entry : proxyMappings.entrySet()) {
            String uri = entry.getKey();
            if (requestUri.startsWith(uri)) {
                if (excludeBaseUri(uri)) {
                    return entry.getValue() + substringAfterLast(requestUri, uri);
                }
                return entry.getValue() + requestUri;
            }
        }
        throw new ResourceNotFoundException("no proxy url found for [" + requestUri + "]");
    }

    private String getDefaultPage(HttpServletRequest request) throws ResourceNotFoundException {
        String requestUri = substringAfter(request.getRequestURI(), request.getContextPath());
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
        if (equalsIgnoreCase(headerName, "x-addama-apikey")) {
            return true;
        }
        if (equalsIgnoreCase(headerName, "x-addama-registry-key")) {
            return true;
        }
        if (equalsIgnoreCase(headerName, "x-addama-registry-host")) {
            return true;
        }
        if (equalsIgnoreCase(headerName, "x-addama-service-uri")) {
            return true;
        }
        if (equalsIgnoreCase(headerName, "x-addama-simpleproxy-transform")) {
            return true;
        }
        if (equalsIgnoreCase(headerName, "content-length")) {
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

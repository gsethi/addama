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
package org.systemsbiology.addama.coresvcs.gae.services;

import com.google.appengine.api.urlfetch.*;
import com.google.apphosting.api.ApiProxy;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline;
import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static com.google.appengine.api.urlfetch.HTTPMethod.POST;

/**
 * @author hrovira
 */
public class Proxy {
    private static final Logger log = Logger.getLogger(Proxy.class.getName());
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";

    private final URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();

    private ApiKeys apiKeys;
    private Users users;

    public void setApiKeys(ApiKeys apiKeys) {
        this.apiKeys = apiKeys;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public void doAction(HttpServletRequest request, HttpServletResponse response, URL targetUrl, UUID accessKey) throws IOException {
        log.info("doAction(" + request.getRequestURI() + "," + targetUrl + ")");

        if (containsFiles(request)) {
            doRedirect(request, response, targetUrl, accessKey);
            return;
        }

        HTTPMethod method = HTTPMethod.valueOf(request.getMethod().toUpperCase());
        HTTPRequest proxyRequest;
        if (method.equals(GET)) {
            proxyRequest = getWithParams(request, targetUrl);
        } else {
            proxyRequest = new HTTPRequest(targetUrl, method, withDeadline(20));
            forwardParameters(request, proxyRequest);
        }

        forwardHeaders(request, proxyRequest, accessKey);

        try {
            doProxy(proxyRequest, response);
        } catch (ResponseTooLargeException e) {
            log.info("doAction(" + request.getRequestURI() + "," + accessKey + "): " + e + ": redirecting instead");
            doRedirect(request, response, targetUrl, accessKey);
        }
    }

    public HTTPRequest getWithParams(HttpServletRequest request, URL url) throws MalformedURLException {
        ArrayList<String> pairs = new ArrayList<String>();

        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            for (String value : request.getParameterValues(parameterName)) {
                pairs.add(parameterName + "=" + value);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append(url.toString());
        if (!pairs.isEmpty()) {
            builder.append("?");

            int i = 0;
            for (String pair : pairs) {
                if (i++ > 0) {
                    builder.append("&");
                }
                builder.append(StringUtils.replace(pair, " ", "+"));
            }
        }

        URL getUrl = new URL(builder.toString());
        log.info("getWithParams(" + url + "):" + getUrl);
        return new HTTPRequest(getUrl, GET, withDeadline(20));
    }

    public void forwardParameters(HttpServletRequest request, HTTPRequest proxyRequest) {
        ArrayList<String> pairs = new ArrayList<String>();

        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            for (String value : request.getParameterValues(parameterName)) {
                pairs.add(parameterName + "=" + value);
            }
        }

        if (!pairs.isEmpty()) {
            StringBuilder builder = new StringBuilder();

            int i = 0;
            for (String pair : pairs) {
                if (i++ > 0) {
                    builder.append("&");
                }
                builder.append(pair);
            }

            proxyRequest.setPayload(builder.toString().getBytes());
        }
    }

    public void forwardHeaders(HttpServletRequest request, HTTPRequest proxyRequest, UUID accessKey) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (!StringUtils.equalsIgnoreCase("x-addama-apikey", headerName)) {
                String headerValue = request.getHeader(headerName);
                headerValue = StringUtils.replace(headerValue, "\n", "");
                headerValue = StringUtils.replace(headerValue, "\r", "");
                proxyRequest.addHeader(new HTTPHeader(headerName, headerValue));
            }
        }

        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-key", accessKey.toString()));
        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_HOST));
        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-user", getUserUri(request)));
    }

    public boolean containsFiles(HttpServletRequest request) {
        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator itr = upload.getItemIterator(request);
                if (itr.hasNext()) {
                    log.warning("containsFiles(" + request.getRequestURI() + "): request has files, must be redirected");
                    return true;
                }
            } catch (Exception e) {
                log.warning("containsFiles(" + request.getRequestURI() + "):" + e);
            }
        }
        return false;
    }

    public void doProxy(HTTPRequest proxyRequest, HttpServletResponse response) throws IOException {
        log.info("doProxy(" + proxyRequest.getURL() + "," + proxyRequest.getMethod() + ")");

        OutputStream outputStream = response.getOutputStream();
        HTTPResponse resp = fetchService.fetch(proxyRequest);
        if (resp != null) {
            log.info("doProxy(" + proxyRequest.getURL() + "," + proxyRequest.getMethod() + "):" + resp.getResponseCode());

            response.setStatus(resp.getResponseCode());
            List<HTTPHeader> headers = resp.getHeaders();
            if (headers != null) {
                for (HTTPHeader header : headers) {
                    response.addHeader(header.getName(), header.getValue());
                }
            }

            outputStream.write(resp.getContent());
        }
    }

    public void doRedirect(HttpServletRequest request, HttpServletResponse response, URL targetUrl, UUID accessKey) throws IOException {
        log.info("doRedirect(" + targetUrl + "): start");

        FetchOptions options = withDeadline(10).doNotFollowRedirects();
        HTTPRequest redirect = new HTTPRequest(new URL(targetUrl.toString() + "/client_redirect"), POST, options);
        redirect.setHeader(new HTTPHeader("x-addama-registry-key", accessKey.toString()));
        redirect.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_HOST));
        redirect.setHeader(new HTTPHeader("x-addama-registry-client", request.getRemoteAddr()));

        HTTPResponse resp = fetchService.fetch(redirect);
        if (resp.getResponseCode() == 302) {
            String location = getLocation(resp);
            log.info("doRedirect(" + targetUrl + "): redirecting to:" + location);
            response.sendRedirect(location);
        } else {
            log.warning("doRedirect(" + targetUrl + "): unable to redirect");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        log.info("doRedirect(" + targetUrl + "): end");
    }

    /*
    * Private Methods
    */

    private String getLocation(HTTPResponse response) {
        for (HTTPHeader header : response.getHeaders()) {
            if (StringUtils.equalsIgnoreCase(header.getName(), "Location")) {
                return header.getValue();
            }
        }
        return null;
    }

    private String getUserUri(HttpServletRequest request) {
        String userUri = users.getLoggedInUserUri();
        if (!StringUtils.isEmpty(userUri)) {
            return userUri;
        }
        String apikey = request.getHeader("x-addama-apikey");
        if (StringUtils.isEmpty(apikey)) {
            apikey = request.getHeader("API_KEY"); // todo : deprecated
        }
        return apiKeys.getUserUriFromApiKey(apikey);
    }
}

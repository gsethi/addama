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
package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.urlfetch.*;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline;
import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static com.google.appengine.api.urlfetch.HTTPMethod.POST;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static java.net.URLEncoder.encode;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.Appspot.APPSPOT_ID;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserEmail;

/**
 * @author hrovira
 */
public class Proxy {
    private static final Logger log = Logger.getLogger(Proxy.class.getName());
    private static final URLFetchService fetchService = getURLFetchService();

    public static void doAction(HttpServletRequest request, HttpServletResponse response, URL targetUrl, UUID accessKey) throws IOException {
        log.info(request.getRequestURI() + "," + targetUrl);

        if (containsFiles(request)) {
            doRedirect(request, response, targetUrl, accessKey);
            return;
        }

        HTTPMethod method = HTTPMethod.valueOf(request.getMethod().toUpperCase());
        HTTPRequest proxyRequest;
        if (method.equals(GET)) {
            proxyRequest = getWithParams(request, targetUrl);
        } else {
            proxyRequest = new HTTPRequest(targetUrl, method, withDeadline(10.0));
            forwardParameters(request, proxyRequest);
        }

        forwardHeaders(request, proxyRequest, accessKey);

        try {
            doProxy(proxyRequest, response);
        } catch (ResponseTooLargeException e) {
            log.info(request.getRequestURI() + "," + accessKey + ": " + e + ": redirecting instead");
            doRedirect(request, response, targetUrl, accessKey);
        } catch (BufferOverflowException e) {
            log.info(request.getRequestURI() + "," + accessKey + ": " + e + ": redirecting instead");
            doRedirect(request, response, targetUrl, accessKey);
        }
    }

    public static HTTPRequest getWithParams(HttpServletRequest request, URL url) throws MalformedURLException {
        Iterable<String> pairs = collectRequestParameters(request);

        StringBuilder builder = new StringBuilder();
        builder.append(url.toString());

        int i = 0;
        for (String pair : pairs) {
            builder.append((i++ == 0) ? "?" : "&");
            builder.append(replace(pair, " ", "+"));
        }

        URL getUrl = new URL(builder.toString());
        log.info(url + ":" + getUrl);
        return new HTTPRequest(getUrl, GET, withDeadline(10.0));
    }

    public static void forwardParameters(HttpServletRequest request, HTTPRequest proxyRequest) {
        Iterable<String> pairs = collectRequestParameters(request);

        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String pair : pairs) {
            if (i++ > 0) builder.append("&");
            builder.append(pair);
        }

        String payload = builder.toString();
        if (!isEmpty(payload)) {
            proxyRequest.setPayload(payload.getBytes());
        }
    }

    public static void forwardHeaders(HttpServletRequest request, HTTPRequest proxyRequest, UUID accessKey) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (!equalsIgnoreCase("x-addama-apikey", headerName)) {
                String headerValue = request.getHeader(headerName);
                headerValue = replace(headerValue, "\n", "");
                headerValue = replace(headerValue, "\r", "");
                proxyRequest.addHeader(new HTTPHeader(headerName, headerValue));
            }
        }

        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-key", accessKey.toString()));
        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_ID));
        proxyRequest.setHeader(new HTTPHeader("x-addama-registry-user", getLoggedInUserEmail(request)));
    }

    public static Iterable<String> collectRequestParameters(HttpServletRequest request) {
        ArrayList<String> pairs = new ArrayList<String>();
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            for (String value : request.getParameterValues(parameterName)) {
                try {
                    pairs.add(parameterName + "=" + encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    log.warning(e.getMessage());
                    pairs.add(parameterName + "=" + value);
                }
            }
        }
        return pairs;
    }

    /*
    * Private Methods
    */

    private static boolean containsFiles(HttpServletRequest request) {
        if (isMultipartContent(request)) {
            try {
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator itr = upload.getItemIterator(request);
                if (itr.hasNext()) {
                    log.warning(request.getRequestURI() + ": request has files, must be redirected");
                    return true;
                }
            } catch (Exception e) {
                log.warning(request.getRequestURI() + ":" + e);
            }
        }
        return false;
    }

    private static void doProxy(HTTPRequest proxyRequest, HttpServletResponse response) throws IOException {
        log.info(proxyRequest.getURL() + "," + proxyRequest.getMethod());

        OutputStream outputStream = response.getOutputStream();
        HTTPResponse resp = fetchService.fetch(proxyRequest);
        if (resp != null) {
            log.info(proxyRequest.getURL() + "," + proxyRequest.getMethod() + ":" + resp.getResponseCode());

            int responseCode = resp.getResponseCode();
            if (responseCode == SC_REQUEST_ENTITY_TOO_LARGE) {
                String location = getLocation(resp);
                log.info(proxyRequest.getURL() + ": too large, redirect:" + location);
                response.sendRedirect(location);
                return;
            }

            response.setStatus(responseCode);
            List<HTTPHeader> headers = resp.getHeaders();
            if (headers != null) {
                for (HTTPHeader header : headers) {
                    response.addHeader(header.getName(), header.getValue());
                }
            }

            outputStream.write(resp.getContent());
        }
    }

    private static void doRedirect(HttpServletRequest request, HttpServletResponse response, URL targetUrl, UUID accessKey) throws IOException {
        log.info(targetUrl + ": start");

        HTTPRequest redirect = new HTTPRequest(new URL(targetUrl.toString() + "/client_redirect"), POST, withDeadline(10.0));
        redirect.setHeader(new HTTPHeader("x-addama-registry-key", accessKey.toString()));
        redirect.setHeader(new HTTPHeader("x-addama-registry-user", getLoggedInUserEmail(request)));
        redirect.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_ID));
        redirect.setHeader(new HTTPHeader("x-addama-registry-client", request.getRemoteAddr()));

        HTTPResponse resp = fetchService.fetch(redirect);
        if (resp.getResponseCode() == SC_FOUND) {
            String location = getLocation(resp);
            log.info(targetUrl + ": redirecting to:" + location);
            response.sendRedirect(location);
        } else {
            log.warning(targetUrl + ": unable to redirect");
            response.setStatus(SC_SERVICE_UNAVAILABLE);
        }
        log.info(targetUrl + ": end");
    }

    private static String getLocation(HTTPResponse response) {
        for (HTTPHeader header : response.getHeaders()) {
            if (equalsIgnoreCase(header.getName(), "Location")) {
                return header.getValue();
            }
        }
        return null;
    }

}

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
package org.systemsbiology.addama.commons.httpclient.support;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class HttpClientTemplate implements InitializingBean {
    private static final Logger log = Logger.getLogger(HttpClientTemplate.class.getName());

    private HttpClient httpClient;
    private HttpState httpState;
    protected String serviceUri;

    /*
     * Constructors
     */

    public HttpClientTemplate() {
    }

    public HttpClientTemplate(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /*
     * Public Getter/Setter Methods
     */

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setHttpState(HttpState httpState) {
        this.httpState = httpState;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }

    /*
     * InitializingBean
     */

    public void afterPropertiesSet() throws Exception {
        if (this.httpClient == null) {
            this.httpClient = new HttpClient();
        }
    }

    /*
     * Public Methods
     */

    public Object executeMethod(HttpMethod method, ResponseCallback responseCallback) throws HttpClientException, HttpClientResponseException {
        synchronized (this) {
            String action = getAction(method);
            try {
                int statusCode = httpClient.executeMethod(method);
                log.info(action + ":" + statusCode);

                return responseCallback.onResponse(statusCode, method);
            } catch (IOException e) {
                log.warning(action + ":" + e);
                throw new HttpClientException(method, e);
            } finally {
                method.releaseConnection();
            }
        }
    }

    public HttpState getHttpState() {
        if (httpState == null) {
            httpState = new HttpState();
            httpClient.setState(httpState);
        }
        return httpState;
    }

    public Cookie[] getCookies() {
        return httpClient.getState().getCookies();
    }

    public void setConnectionTimeout(int timeoutLength) {
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeoutLength);
    }

    public void addCookies(Cookie... cookies) {
        if (cookies == null || cookies.length == 0) return;

        HttpState initialState = getHttpState();
        for (Cookie cookie : cookies) {
            initialState.addCookie(cookie);
        }

        HttpClientParams params = httpClient.getParams();
        params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    /*
     * Private Methods
     */

    private String getAction(HttpMethod method) throws HttpClientException {
        try {
            return method.getName() + " " + method.getURI().getPath();
        } catch (URIException e) {
            throw new HttpClientException(method, e);
        }
    }
}

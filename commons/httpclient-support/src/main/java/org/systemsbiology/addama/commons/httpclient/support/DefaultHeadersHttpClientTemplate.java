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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Map;

/**
 * @author hrovira
 */
public class DefaultHeadersHttpClientTemplate extends HttpClientTemplate {
    private Map<String, String> headers;

    public DefaultHeadersHttpClientTemplate() {
    }

    public DefaultHeadersHttpClientTemplate(HttpClient httpClient) {
        super(httpClient);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object executeMethod(HttpMethod httpMethod, ResponseCallback responseCallback)
            throws HttpClientException, HttpClientResponseException {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpMethod.setRequestHeader(entry.getKey(), entry.getValue());
            }
        }

        return super.executeMethod(httpMethod, responseCallback);
    }
}
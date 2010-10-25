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
import org.apache.commons.lang.StringUtils;

/**
 * @author hrovira
 */
public class ApiKeyHttpClientTemplate extends HttpClientTemplate {
    private String apikey;

    public ApiKeyHttpClientTemplate() {
    }

    public ApiKeyHttpClientTemplate(HttpClient httpClient) {
        super(httpClient);
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public Object executeMethod(HttpMethod httpMethod, ResponseCallback responseCallback)
            throws HttpClientException, HttpClientResponseException {
        if (!StringUtils.isEmpty(serviceUri)) {
            httpMethod.setRequestHeader("x-addama-service-uri", serviceUri);
        }
        httpMethod.setRequestHeader("x-addama-apikey", apikey);
        return super.executeMethod(httpMethod, responseCallback);
    }
}
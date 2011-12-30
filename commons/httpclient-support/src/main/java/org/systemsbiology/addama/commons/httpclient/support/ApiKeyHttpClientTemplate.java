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
import org.systemsbiology.addama.commons.spring.PropertiesFileLoader;

import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class ApiKeyHttpClientTemplate extends HttpClientTemplate {
    private static final Logger log = Logger.getLogger(ApiKeyHttpClientTemplate.class.getName());

    private static final String KEY = "httpclient.apikey";

    private PropertiesFileLoader propertiesFileLoader;
    private String apikey;

    public ApiKeyHttpClientTemplate() {
    }

    public ApiKeyHttpClientTemplate(HttpClient httpClient) {
        super(httpClient);
    }

    public void setPropertiesFileLoader(PropertiesFileLoader propertiesFileLoader) {
        this.propertiesFileLoader = propertiesFileLoader;
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (propertiesFileLoader.loaded() && propertiesFileLoader.has(KEY)) {
            this.apikey = propertiesFileLoader.getProperty(KEY);
            return;
        }
        log.warning("api key not configured in 'addama.properties' [" + KEY + "]");
    }

    public Object executeMethod(HttpMethod httpMethod, ResponseCallback responseCallback)
            throws HttpClientException, HttpClientResponseException {
        if (!isEmpty(serviceUri)) {
            httpMethod.setRequestHeader("x-addama-service-uri", serviceUri);
        }
        if (!isEmpty(apikey)) {
            httpMethod.setRequestHeader("x-addama-apikey", apikey);
        }
        return super.executeMethod(httpMethod, responseCallback);
    }
}
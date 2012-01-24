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
package org.systemsbiology.addama.services.proxy.callbacks;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;
import org.systemsbiology.addama.services.proxy.transforms.ResponseTransform;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.pipe;

/**
 * @author hrovira
 */
public class SimpleProxyResponseCallback implements ResponseCallback {
    private static final Logger log = Logger.getLogger(SimpleProxyResponseCallback.class.getName());

    private final HttpServletResponse response;
    private int bufferSize = 2048;
    private String defaultPage;
    private ResponseTransform responseTransform;

    public SimpleProxyResponseCallback(HttpServletResponse response) {
        this(response, null);
    }

    public SimpleProxyResponseCallback(HttpServletResponse response, String defaultPage) {
        this.response = response;
        this.defaultPage = defaultPage;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setResponseTransform(ResponseTransform responseTransform) {
        this.responseTransform = responseTransform;
    }

    public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
        try {
            if (statusCode == 404) {
                if (!StringUtils.isEmpty(defaultPage)) {
                    this.response.sendRedirect(defaultPage);
                    return null;
                }
            }

            this.response.setStatus(statusCode);
            if (statusCode == 302) {
                Header location = method.getResponseHeader("Location");
                if (location != null) {
                    URL locationURL = new URL(location.getValue());
                    String locationUri = locationURL.getPath();
                    log.info(statusCode + ": redirect=[" + locationUri + "]");
                    this.response.sendRedirect(locationUri);
                    return null;
                }
            }

            InputStream inputStream = new BufferedInputStream(method.getResponseBodyAsStream(), bufferSize);
            if (responseTransform != null) {
                responseTransform.handle(inputStream, response);
                return null;
            }

            Header contentType = method.getResponseHeader("Content-Type");
            if (contentType != null) {
                this.response.setContentType(contentType.getValue());
            }

            for (Header header : method.getResponseHeaders()) {
                String headerName = header.getName();
                if (!equalsIgnoreCase("Transfer-Encoding", headerName) && !equalsIgnoreCase("Content-Type", headerName)) {
                    this.response.setHeader(headerName, header.getValue());
                }
            }

            pipe(inputStream, response.getOutputStream());
        } catch (Exception e) {
            throw new HttpClientResponseException(statusCode, method, e);
        }

        return null;
    }
}
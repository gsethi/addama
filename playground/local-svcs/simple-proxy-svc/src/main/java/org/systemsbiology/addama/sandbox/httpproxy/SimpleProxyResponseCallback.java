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
package org.systemsbiology.addama.sandbox.httpproxy;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class SimpleProxyResponseCallback implements ResponseCallback {
    private static final Logger log = Logger.getLogger(SimpleProxyResponseCallback.class.getName());

    private final HttpServletResponse response;
    private int bufferSize = 2048;
    private String defaultPage;

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
                    log.info("onResponse(" + statusCode + "): redirect=[" + locationUri + "]");
                    this.response.sendRedirect(locationUri);
                    return null;
                }
            }

            Header contentType = method.getResponseHeader("Content-Type");
            log.info("contentType=" + contentType);
            if (contentType != null) {
                log.info("contentType.getValue=" + contentType.getValue());
                this.response.setContentType(contentType.getValue());
            }

            InputStream inputStream = new BufferedInputStream(method.getResponseBodyAsStream(), bufferSize);
            OutputStream outputStream = response.getOutputStream();

            byte buffer[] = new byte[bufferSize];
            while (true) {
                int len = inputStream.read(buffer);
                if (len == -1) {
                    break;
                }

                outputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            throw new HttpClientResponseException(statusCode, method, e);
        }

        return null;
    }
}
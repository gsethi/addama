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
package org.systemsbiology.addama.appengine.pojos;

import com.google.appengine.api.urlfetch.*;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.HTTPMethod.GET;
import static com.google.appengine.api.urlfetch.HTTPMethod.HEAD;

/**
 * @author hrovira
 *         TODO aggregate response headers
 */
public class MapReduceTooLargeHTTPResponse {
    private static final Logger log = Logger.getLogger(MapReduceTooLargeHTTPResponse.class.getName());

    private final URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();

    private int splitSize = 500000;
    private boolean splitOnlyOnSuccessResponseCodes;

    public void setSplitSize(int splitSize) {
        this.splitSize = splitSize;
    }

    public void setSplitOnlyOnSuccessResponseCodes(boolean flag) {
        this.splitOnlyOnSuccessResponseCodes = flag;
    }

    public HTTPResponse fetch(HTTPRequest request, OutputStream outputStream) throws IOException {
        URL requestUrl = request.getURL();
        HTTPMethod method = request.getMethod();
        if (!GET.equals(method)) {
            log.warning(requestUrl + ": Will not attempt to range split on " + method);
            return fetchService.fetch(request);
        }

        HTTPResponse headResponse = head(request);
        Long contentLength = getContentLength(headResponse);
        if (contentLength == null) {
            log.warning(requestUrl + ": Unable to determine content length from HEAD");
            return fetchService.fetch(request);
        }

        if (contentLength <= splitSize) {
            log.fine(requestUrl + ": Content NOT too large: " + contentLength);
            return fetchService.fetch(request);
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(contentLength.intValue());
            mapReduce(request, baos, contentLength);
            outputStream.write(baos.toByteArray());
            return headResponse;
        } catch (Exception ex) {
            log.warning(requestUrl + ": Range Split Failed: " + ex);
        }

        return fetchService.fetch(request);
    }

    /*
     * Protected Methods
     */

    protected void mapReduce(HTTPRequest request, OutputStream outputStream, long contentLength) throws Exception {
        List<Future<HTTPResponse>> futureResponses = new ArrayList<Future<HTTPResponse>>();

        long loopEnd = splitSize;
        for (long loopStart = 0; loopStart < contentLength;) {
            if (loopEnd > contentLength) {
                loopEnd = contentLength;
            }

            futureResponses.add(map(request, loopStart, loopEnd));

            loopStart = loopEnd + 1;
            loopEnd += splitSize;
        }

        reduce(futureResponses, outputStream);
    }

    protected Future<HTTPResponse> map(HTTPRequest request, long start, long end) {
        log.fine(request.getURL() + "," + start + "," + end);

        HTTPRequest splitRequest = new HTTPRequest(request.getURL(), request.getMethod(), request.getFetchOptions());
        for (HTTPHeader header : request.getHeaders()) {
            if (!StringUtils.equalsIgnoreCase(header.getName(), ("Range"))) {
                splitRequest.addHeader(header);
            }
        }
        splitRequest.setHeader(new HTTPHeader("Range", "bytes=" + start + "-" + end));
        splitRequest.setPayload(request.getPayload());

        return fetchService.fetchAsync(splitRequest);
    }

    protected void reduce(List<Future<HTTPResponse>> futureResponses, OutputStream outputStream)
            throws IOException, ExecutionException, InterruptedException {
        for (Future<HTTPResponse> futureResponse : futureResponses) {
            HTTPResponse response = futureResponse.get();
            int responseCode = response.getResponseCode();

            if (this.splitOnlyOnSuccessResponseCodes) {
                String statusCode = "" + responseCode;
                if (!statusCode.startsWith("2")) {
                    throw new IOException("response code not success: " + responseCode);
                }
            }

            byte[] content = response.getContent();
            if (content != null) {
                outputStream.write(content);
                outputStream.flush();
            }
        }
    }

    /*
     * Private Methods
     */

    private HTTPResponse head(HTTPRequest request) throws IOException {
        URL requestUrl = request.getURL();
        HTTPRequest headRequest = new HTTPRequest(requestUrl, HEAD);
        for (HTTPHeader header : request.getHeaders()) {
            headRequest.addHeader(header);
        }
        headRequest.setPayload(request.getPayload());

        return fetchService.fetch(headRequest);
    }

    private Long getContentLength(HTTPResponse response) {
        if (response != null) {
            for (HTTPHeader header : response.getHeaders()) {
                if (StringUtils.equalsIgnoreCase(header.getName(), "Content-Length")) {
                    return Long.parseLong(header.getValue());
                }
                if (StringUtils.equalsIgnoreCase(header.getName(), "x-addama-content-length")) {
                    return Long.parseLong(header.getValue());
                }
            }
        }
        return null;
    }
}

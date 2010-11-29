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
package org.systemsbiology.addama.coresvcs.gae.filters.callbacks;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import org.systemsbiology.addama.commons.gae.http.MapReduceTooLargeHTTPResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;

/**
 * @author hrovira
 */
public abstract class MapReducingResponder {
    private final MapReduceTooLargeHTTPResponse mapReduce = new MapReduceTooLargeHTTPResponse();

    protected HTTPResponseContent mapReduce(HTTPRequest request) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPResponse resp = mapReduce.fetch(request, outputStream);
        if (outputStream.size() > 0) {
            return new HTTPResponseContent(resp, outputStream.toByteArray());
        }
        if (resp.getResponseCode() == HttpServletResponse.SC_OK) {
            return new HTTPResponseContent(resp);
        }
        return null;
    }

}

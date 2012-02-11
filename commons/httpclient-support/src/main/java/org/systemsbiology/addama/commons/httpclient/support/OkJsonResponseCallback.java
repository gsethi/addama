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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

/**
 * @author hrovira
 */
public class OkJsonResponseCallback implements ResponseCallback {

    public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
        if (statusCode == 200) {
            try {
                Header contentType = method.getResponseHeader("Content-Type");
                if (contentType != null && StringUtils.contains(contentType.getValue(), "application/json")) {
                    JSONObject json = new JSONObject(method.getResponseBodyAsString());
                    return onResponse(statusCode, method, json);
                } else {
                    throw new Exception("onResponse(" + statusCode + "): response does not contain json");
                }
            } catch (Exception e) {
                throw new HttpClientResponseException(statusCode, method, e);
            }
        }
        return null;
    }

    public Object onResponse(int statusCode, HttpMethod method, JSONObject json) throws Exception {
        return json;
    }

}
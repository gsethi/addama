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
package org.systemsbiology.addama.rest.util;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;

import static org.systemsbiology.addama.rest.AddamaKeywords.addama_uri;

/**
 * @author hrovira
 */
public class UriBuilder {
    private final String baseUri;

    public UriBuilder(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (StringUtils.contains(requestUri, "/uuid")) {
            this.baseUri = StringUtils.substringBetween(requestUri, request.getContextPath(), "/uuid");
        } else if (StringUtils.contains(requestUri, "/path")) {
            this.baseUri = StringUtils.substringBetween(requestUri, request.getContextPath(), "/path");
        } else {
            this.baseUri = StringUtils.substringAfter(requestUri, request.getContextPath());
        }
    }

    public String getUri(Node node) throws RepositoryException {
        if (node == null) return null;

        if (node.hasProperty(addama_uri.word())) {
            Property addamaUri = node.getProperty(addama_uri.word());
            return addamaUri.getString();
        }

        if (node.hasProperty("ref-repo") && node.hasProperty("ref-path")) {
            throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
        } else if (node.hasProperty("ref-uri")) {
            throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
        }

        return getPathUri(node);
    }

    public void putUri(Node node, JSONObject json) throws RepositoryException, JSONException {
        if (node == null || json == null) return;

        if (node.hasProperty(addama_uri.word())) {
            Property addamaUri = node.getProperty(addama_uri.word());
            String uri = addamaUri.getString();
            json.put("uri", uri);
            json.put("isReference", true);

        } else {
            json.put("uri", getPathUri(node));
            json.put("uri-uuid", getUuidUri(node));
        }

        if (node.hasProperty("ref-repo") && node.hasProperty("ref-path")) {
            throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");

        } else if (node.hasProperty("ref-uri")) {
            throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");
        }
    }

    public String getPathUri(Node node) throws RepositoryException {
        // TODO : encoding path is problematic
        if (node == null) return null;
        String path = node.getPath();
        if (StringUtils.isEmpty(path) || "/".equals(path)) {
            return this.baseUri;
        }
        return this.baseUri + "/path" + path;
    }

    public URI getPathURI(String path) {
        // TODO : encoding path is problematic
        if (StringUtils.isEmpty(path) || "/".equals(path)) {
            return URI.create(this.baseUri);
        }
        String encpath = path.replaceAll(" ", "%20");
        return URI.create(this.baseUri + "/path" + encpath);
    }

    /*
    * Private Methods
    */

    public String getUuidUri(Node node) throws RepositoryException {
        if (node.hasProperty("jcr:uuid")) {
            return this.baseUri + "/uuid/" + node.getUUID();
        }
        return null;
    }
}

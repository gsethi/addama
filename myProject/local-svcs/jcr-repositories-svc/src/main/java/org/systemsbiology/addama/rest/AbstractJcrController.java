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
package org.systemsbiology.addama.rest;

import org.apache.commons.lang.StringUtils;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public abstract class AbstractJcrController {
    protected final Logger log = Logger.getLogger(getClass().getName());

    protected DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    /*
     * Dependency Injection
     */

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /*
     * Protected Methods
     */

    protected Node getNode(HttpServletRequest request, String suffix) throws RepositoryException, ResourceNotFoundException {
        JcrTemplate jcrTemplate = getJcrTemplate(request);

        String requestUri = getDecodedRequestUri(request);
        if (StringUtils.contains(requestUri, "/path")) {
            String path = getPath(request, suffix);
            log.fine("getNode:" + path);
            if (!jcrTemplate.itemExists(path)) {
                throw new ResourceNotFoundException(path);
            }
            return (Node) jcrTemplate.getItem(path);
        } else if (StringUtils.contains(requestUri, "/uuid")) {
            String uuid = StringUtils.substringAfter(requestUri, "/uuid/");
            log.fine("getNode:" + uuid);
            return jcrTemplate.getNodeByUUID(uuid);
        } else {
            log.fine("getNode: rootNode");
            return jcrTemplate.getRootNode();
        }
    }

    protected String getPath(HttpServletRequest request, String suffix) throws RepositoryException, ResourceNotFoundException {
        String requestUri = getDecodedRequestUri(request);
        if (StringUtils.contains(requestUri, "/path")) {
            if (StringUtils.isEmpty(suffix)) suffix = null;

            String path = StringUtils.substringAfter(requestUri, "/path");
            if (StringUtils.contains(requestUri, suffix)) {
                path = StringUtils.substringBetween(requestUri, "/path", suffix);
            }

            if (StringUtils.isEmpty(path)) path = "/";
            log.fine("getPath:" + path);
            return path;
        }

        if (StringUtils.contains(requestUri, "/uuid")) {
            JcrTemplate jcrTemplate = getJcrTemplate(request);

            String uuid = StringUtils.substringAfter(requestUri, "/uuid/");
            Node node = jcrTemplate.getNodeByUUID(uuid);
            String path = node.getPath();
            log.fine("getPath(" + uuid + "):" + path);
            return path;
        }

        log.fine("getPath: root");
        return "/";
    }

    protected JcrTemplate getJcrTemplate(HttpServletRequest request) throws ResourceNotFoundException {
        return (JcrTemplate) request.getAttribute("JCR_TEMPLATE");
    }

    /*
    * Private Methods
    */

    private String getDecodedRequestUri(HttpServletRequest request) {
        try {
            return URLDecoder.decode(request.getRequestURI(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

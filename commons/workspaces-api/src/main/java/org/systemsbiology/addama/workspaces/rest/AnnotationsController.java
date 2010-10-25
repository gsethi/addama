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
package org.systemsbiology.addama.workspaces.rest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class AnnotationsController extends BaseController {
    private static final Logger log = Logger.getLogger(AnnotationsController.class.getName());

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = getUri(request);
        requestUri = StringUtils.substringBeforeLast(requestUri, "/annotations");

        log.info("annotations for:" + requestUri);

        String nodePath = getNodePath(requestUri);
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (!jcrTemplate.itemExists(nodePath)) {
            throw new ResourceNotFoundException(requestUri);
        }

        JSONObject json = new JSONObject();
        Node node = (Node) jcrTemplate.getItem(nodePath);
        appendAnnotations(node, json);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }
}

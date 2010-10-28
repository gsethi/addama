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
package org.systemsbiology.addama.services.repositories.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class ParentController extends BaseController {
    private static final Logger log = Logger.getLogger(ParentController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView parent(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        String uri = StringUtils.substringBetween(requestUri, request.getContextPath(), "/parent");
        String baseUri = StringUtils.substringBeforeLast(uri, "/");
        if (StringUtils.contains(uri, "/path")) {
            baseUri = StringUtils.substringBefore(uri, "/path");
        }

        Node node = getNode(request, "/parent");
        Node parentNode = NodeUtil.getParent(node);
        if (parentNode == null) {
            throw new ResourceNotFoundException(baseUri);
        }

        JSONObject json = new JSONObject();

        String parentPath = parentNode.getPath();
        if (StringUtils.equals(parentPath, "/") || StringUtils.isEmpty(parentPath)) {
            json.put("uri", baseUri);
            json.put("name", "root");
        } else {
            json.put("uri", baseUri + "/path" + parentNode.getPath());
            json.put("name", parentNode.getName());
        }

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

}
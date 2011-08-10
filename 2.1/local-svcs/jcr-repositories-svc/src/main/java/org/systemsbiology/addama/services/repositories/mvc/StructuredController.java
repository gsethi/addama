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

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class StructuredController extends BaseController {
    private static final Logger log = Logger.getLogger(StructuredController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView structured(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        Node node = getNode(request, "/structured");
        JSONObject json = new JSONObject();
        appendStructure(node, json, request);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private void appendStructure(Node node, JSONObject json, HttpServletRequest request) throws Exception {
        loadProperties(node, json);

        NodeIterator itr = node.getNodes();
        while (itr.hasNext()) {
            Node nextNode = itr.nextNode();

            String name = nextNode.getName();
            JSONObject item = new JSONObject();
            if (markAsFile(nextNode, item)) {
                loadSelectedProperties(nextNode, item, request);
                json.accumulate(name, item);
            } else {
                appendStructure(nextNode, item, request);
                json.accumulate(name, item);
            }
        }
    }
}

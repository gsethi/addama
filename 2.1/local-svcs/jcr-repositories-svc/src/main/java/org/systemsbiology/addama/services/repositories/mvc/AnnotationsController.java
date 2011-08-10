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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class AnnotationsController extends BaseController {
    private static final Logger log = Logger.getLogger(AnnotationsController.class.getName());

    @RequestMapping(value = "/**/annotations", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView annotations(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        Node node = getNode(request, "/annotations");

        JSONObject json = new JSONObject();
        json.put("uri", StringUtils.substringBetween(requestUri, request.getContextPath(), "/annotations"));

        loadProperties(node, json);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/annotations/terms", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView annotations_terms(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        Node node = getNode(request, "/annotations/terms");

        JSONObject json = new JSONObject();
        json.put("uri", StringUtils.substringBetween(requestUri, request.getContextPath(), "/annotations"));

        appendTerms(node, json);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/annotations", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView annotations_post(HttpServletRequest request, @RequestParam("annotations") String json) throws Exception {
        JSONObject annotationsJson = new JSONObject(json);

        Node node = getNode(request, "/annotations");

        CreateOrUpdateNode createNode = new CreateOrUpdateNode();
        createNode.doUpdate(node, annotationsJson);

        return new ModelAndView(new OkResponseView());
    }

    /*
     * Private Methods
     */

    private void appendTerms(Node node, JSONObject json) throws Exception {
        PropertyIterator itr = node.getProperties();
        while (itr.hasNext()) {
            Property prop = itr.nextProperty();
            String propName = prop.getName();
            if (!propName.startsWith("jcr:")) {
                json.append("terms", prop.getName());
            }

        }
    }
}
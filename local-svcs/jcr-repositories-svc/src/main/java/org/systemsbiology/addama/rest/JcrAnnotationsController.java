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

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.rest.json.NodeAnnotationsJSONObject;
import org.systemsbiology.addama.rest.json.NodeAnnotationsTermsJSONObject;
import org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode;
import org.systemsbiology.addama.rest.views.PostJsonView;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

import static org.systemsbiology.addama.jcr.support.JcrTemplateProvider.getJcrTemplate;

/**
 * @author hrovira
 */
@Controller
public class JcrAnnotationsController extends AbstractJcrController {

    @RequestMapping(value = "/**/annotations", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView annotations(HttpServletRequest request) throws Exception {
        Node node = getNode(request, "/annotations");

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", new NodeAnnotationsJSONObject(node, request, dateFormat));
        return mav;
    }

    @RequestMapping(value = "/**/annotations/terms", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView annotations_terms(HttpServletRequest request) throws Exception {
        Node node = getNode(request, "/annotations/terms");

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", new NodeAnnotationsTermsJSONObject(node, request, dateFormat));
        return mav;
    }

    @RequestMapping(value = "/**/annotations", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView annotations_post(HttpServletRequest request, @RequestParam("JSON") String json) throws Exception {
        JSONObject annotationsJson = new JSONObject(json);

        String path = getPath(request, "/annotations");
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (jcrTemplate != null && jcrTemplate.itemExists(path)) {
            Node node = (Node) jcrTemplate.getItem(path);
            CreateOrUpdateNode createNode = new CreateOrUpdateNode();
            createNode.doUpdate(node, annotationsJson);

            ModelAndView mav = new ModelAndView(new PostJsonView());
            mav.addObject("json", new NodeAnnotationsJSONObject(node, request, dateFormat));
            mav.addObject("node", node);
            return mav;
        }

        throw new ResourceNotFoundException(request.getRequestURI());
    }
}
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

import org.json.JSONArray;
import org.json.JSONException;
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
import org.systemsbiology.addama.commons.web.views.ResourceNotFoundView;
import org.systemsbiology.addama.rest.json.NodeLabelsJSONObject;
import org.systemsbiology.addama.rest.views.PostJsonView;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

import static org.systemsbiology.addama.jcr.support.JcrTemplateProvider.getJcrTemplate;

/**
 * @author hrovira
 */
@Controller
public class JcrLabelsController extends AbstractJcrController {

    @RequestMapping(value = "/**/labels", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView labels(HttpServletRequest request) throws Exception {
        log.info("labels");

        Node node = getNode(request, "/labels");

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", new NodeLabelsJSONObject(node, request, dateFormat));
        return mav;
    }

    @RequestMapping(value = "/**/labels", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView labelsManage(HttpServletRequest request, @RequestParam("JSON") String json) throws Exception {
        log.info("labels(" + json + ")");

        String path = getPath(request, "/labels");
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (jcrTemplate != null && jcrTemplate.itemExists(path)) {
            Node node = (Node) jcrTemplate.getItem(path);
            Set<String> set = new HashSet<String>(); //getExistingLabels(node);
            addNewLabels(new JSONObject(json), set);
            node.setProperty("labels", set.toArray(new String[set.size()]));

            ModelAndView mav = new ModelAndView(new PostJsonView());
            mav.addObject("json", new NodeLabelsJSONObject(node, request, dateFormat));
            mav.addObject("node", node);
            return mav;
        }
        throw new ResourceNotFoundException(request.getRequestURI());
    }

    @RequestMapping(value = "/**/labels/append", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView labelsAppend(HttpServletRequest request, @RequestParam("JSON") String json) throws Exception {
        log.info("labels(" + json + ")");

        String path = getPath(request, "/labels/append");
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (!jcrTemplate.itemExists(path)) {
            ModelAndView mav = new ModelAndView(new ResourceNotFoundView());
            mav.addObject("error_message", "Specified resource not found at " + path);
            return mav;
        }

        Node node = (Node) jcrTemplate.getItem(path);
        Set<String> set = getExistingLabels(node);
        addNewLabels(new JSONObject(json), set);
        node.setProperty("labels", set.toArray(new String[set.size()]));

        ModelAndView mav = new ModelAndView(new PostJsonView());
        mav.addObject("json", new NodeLabelsJSONObject(node, request, dateFormat));
        mav.addObject("node", node);
        return mav;
    }

    private Set<String> getExistingLabels(Node node) throws RepositoryException {
        HashSet<String> set = new HashSet<String>();
        if (node.hasProperty("labels")) {
            Property labelsProperty = node.getProperty("labels");
            for (Value value : labelsProperty.getValues()) {
                set.add(value.getString());
            }
        }
        return set;
    }

    private void addNewLabels(JSONObject labelsJson, Set<String> set) throws JSONException {
        JSONArray newValues = labelsJson.getJSONArray("labels");
        for (int i = 0; i < newValues.length(); i++) {
            set.add(newValues.getString(i));
        }
    }
}
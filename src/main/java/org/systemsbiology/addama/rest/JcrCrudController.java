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
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.ResourceNotFoundView;
import org.systemsbiology.addama.commons.web.views.ResourceStateConflictView;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;
import org.systemsbiology.addama.rest.json.BaseNodeJSONObject;
import org.systemsbiology.addama.rest.json.NodeMetaJSONObject;
import org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode;
import org.systemsbiology.addama.rest.views.PostJsonView;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class JcrCrudController extends AbstractJcrController {
    private static final Logger log = Logger.getLogger(JcrCrudController.class.getName());

    /*
    * Controller Methods
    */

    @RequestMapping(value = "/**/path/**", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView retrieveByPath(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String path = getPath(request, null);

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if ("/".equals(path)) {
            JSONObject json = new NodeMetaJSONObject(jcrTemplate.getRootNode(), request, dateFormat);
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        if (!jcrTemplate.itemExists(path)) {
            throw new ResourceNotFoundException(path);
        }

        Node node = (Node) jcrTemplate.getItem(path);
        return new ModelAndView(new JsonItemsView()).addObject("json", new NodeMetaJSONObject(node, request, dateFormat));
    }

    @RequestMapping(value = "/**/uuid/**", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView retrieveByUuid(HttpServletRequest request) throws Exception {
        log.info("retrieveByUuid");

        Node node = getNode(request, null);

        return new ModelAndView(new JsonItemsView()).addObject("json", new NodeMetaJSONObject(node, request, dateFormat));
    }

    @RequestMapping(value = "/**/path/**", method = RequestMethod.PUT)
    @ModelAttribute
    public ModelAndView createByPut(HttpServletRequest request, @RequestParam(value = "JSON", required = false) String json) throws Exception {
        return createResource(request, null, json);
    }

    @RequestMapping(value = "/**/path/**/create", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView createByPost(HttpServletRequest request, @RequestParam(value = "JSON", required = false) String json) throws Exception {
        return createResource(request, "/create", json);
    }

    @RequestMapping(value = "/**/path/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView updateByPath(HttpServletRequest request, @RequestParam(value = "JSON", required = false) String json) throws Exception {
        log.info(json);

        String path = getPath(request, null);
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (!jcrTemplate.itemExists(path)) {
            ModelAndView mav = new ModelAndView(new ResourceNotFoundView());
            mav.addObject("error_message", "Specified resource not found at " + path);
            return mav;
        }

        Node node = (Node) jcrTemplate.getItem(path);

        if (StringUtils.isEmpty(json)) json = "{}";
        JSONObject jsonObject = new JSONObject(json);

        CreateOrUpdateNode createNode = new CreateOrUpdateNode();
        createNode.doUpdate(node, jsonObject);

        ModelAndView mav = new ModelAndView(new PostJsonView());
        mav.addObject("json", new NodeMetaJSONObject(node, request, dateFormat));
        mav.addObject("node", node);
        return mav;
    }

    @RequestMapping(value = "/**/uuid/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView updateByUuid(HttpServletRequest request, @RequestParam(value = "JSON", required = false) String json) throws Exception {
        log.info(json);

        Node node = getNode(request, null);

        if (StringUtils.isEmpty(json)) json = "{}";

        CreateOrUpdateNode updateNode = new CreateOrUpdateNode();
        updateNode.doUpdate(node, new JSONObject(json));

        ModelAndView mav = new ModelAndView(new PostJsonView());
        mav.addObject("json", new NodeMetaJSONObject(node, request, dateFormat));
        mav.addObject("node", node);
        return mav;
    }

    @RequestMapping(value = "/**/uuid/*/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView delete_uuid_post(HttpServletRequest request) throws Exception {
        log.info("delete_uuid_post");

        Node node = getNode(request, "/delete");
        return delete(node, request);
    }

    @RequestMapping(value = "/**/path/**/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView delete_path_post(HttpServletRequest request) throws Exception {
        log.info("delete_path_post");

        Node node = getNode(request, "/delete");
        return delete(node, request);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView delete_delete(HttpServletRequest request) throws Exception {
        log.info("delete_delete");

        Node node = getNode(request, null);
        return delete(node, request);
    }

    /*
     * Private Methods
     */

    private ModelAndView createResource(HttpServletRequest request, String suffix, String json) throws Exception {
        log.info(request.getRequestURI() + "," + suffix);

        String path = getPath(request, suffix);
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (jcrTemplate.itemExists(path)) {
            return new ModelAndView(new ResourceStateConflictView());
        }

        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(path));

        if (StringUtils.isEmpty(json)) json = "{}";
        JSONObject jsonObject = new JSONObject(json);

        CreateOrUpdateNode createNode = new CreateOrUpdateNode();
        createNode.doCreate(node, jsonObject);

        ModelAndView mav = new ModelAndView(new PostJsonView());
        mav.addObject("json", new NodeMetaJSONObject(node, request, dateFormat));
        mav.addObject("node", node);
        return mav;
    }

    private ModelAndView delete(Node node, HttpServletRequest request) throws Exception {
        JSONObject json = new BaseNodeJSONObject(node, request, dateFormat);

        node.remove();

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }
}
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
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;
import org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class NodeCrudController extends BaseController {
    private static final Logger log = Logger.getLogger(NodeCrudController.class.getName());

    /*
    * Controller Methods
    */

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        Node node = getNode(request, null);
        JSONObject json = constructNodeMetaJson(request, null, node);

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**", method = RequestMethod.POST)
    public ModelAndView post(HttpServletRequest request, @RequestParam(value = "JSON", required = false) String json) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        String path = getPath(request, null);

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(path));

        if (StringUtils.isEmpty(json)) json = "{}";

        CreateOrUpdateNode createNode = new CreateOrUpdateNode();
        createNode.doUpdate(node, new JSONObject(json));
        JSONObject responseJson = constructNodeMetaJson(request, null, node);

        return new ModelAndView(new JsonItemsView()).addObject("json",
                                                               responseJson);
    }

    @RequestMapping(value = "/**/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView delete_by_post(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        return deleteNode(request, "/delete");
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView delete_by_delete(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        return deleteNode(request, null);
    }

    /*
     * Private Methods
     */

    private ModelAndView deleteNode(HttpServletRequest request, String suffix) throws Exception {
        Node node = getNode(request, suffix);
        node.remove();

        JSONObject json = new JSONObject();
        json.put("uri", StringUtils.substringAfter(request.getRequestURI(), request.getContextPath()));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    private String getPath(HttpServletRequest request, String suffix) throws RepositoryException, ResourceNotFoundException {
        String requestUri = request.getRequestURI();
        if (StringUtils.contains(requestUri, "/path")) {
            if (StringUtils.isEmpty(suffix)) suffix = null;

            String path = StringUtils.substringAfter(requestUri, "/path");
            if (StringUtils.contains(requestUri, suffix)) {
                path = StringUtils.substringBetween(requestUri, "/path", suffix);
            }

            if (StringUtils.isEmpty(path)) path = "/";
            return path;
        }

        if (StringUtils.contains(requestUri, "/uuid")) {
            JcrTemplate jcrTemplate = getJcrTemplate(request);

            String uuid = StringUtils.substringAfter(requestUri, "/uuid/");
            Node node = jcrTemplate.getNodeByUUID(uuid);
            return node.getPath();
        }

        return "/";
    }

    private JSONObject constructNodeMetaJson(HttpServletRequest request,
                                             String uriSuffix,
                                             Node node) throws Exception {

        uriSuffix = (null == uriSuffix) ? "/" : uriSuffix;
        String baseUri = StringUtils.chomp(StringUtils.substringAfter
                                           (request.getRequestURI(),
                                            request.getContextPath()),
                                           uriSuffix);

        JSONObject ops = new JSONObject();
        ops.put("annotations", baseUri + "/annotations");
        ops.put("terms", baseUri + "/annotations/terms");
        ops.put("meta", baseUri + "/meta");
        ops.put("directory", baseUri + "/dir");

        JSONObject json = new JSONObject();
        json.put("uri", baseUri);
        json.put("operations", ops);
        markAsFile(node, json);
        loadProperties(node, json);
        appendItems(baseUri, node, json, request);

        return json;
    }

}
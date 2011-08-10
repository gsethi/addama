/*
 * Copyright (C) 2003-2010 Institute for Systems Biology
 *                          Seattle, Washington, USA.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package org.systemsbiology.addama.services.repositories.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.FilesDirectoriesJsonView;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;


/**
 * @author hrovira
 */
@Controller
public class DirectoryController extends BaseController {
    private static final Logger log = Logger.getLogger(DirectoryController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView directory(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        String baseUri = StringUtils.substringAfter(requestUri, request.getContextPath());
        if (StringUtils.contains(requestUri, "/path")) {
            baseUri = StringUtils.substringBefore(baseUri, "/path");
        }

        Node node = getNode(request, "/dir");
        JSONObject json = new JSONObject();

        appendParent(baseUri, node, json);
        appendChildren(baseUri, node, json, request);
        loadSelectedProperties(node, json, request);

        return new ModelAndView(new FilesDirectoriesJsonView()).addObject("json", json);
    }

    /*
    * Private Methods
    */

    private void appendParent(String baseUri, Node node, JSONObject json) throws RepositoryException, JSONException {
        if (!StringUtils.equals(node.getPath(), "/")) {
            Node parentNode = NodeUtil.getParent(node);
            if (parentNode != null) {
                String parentpath = parentNode.getPath();
                if (StringUtils.equals(parentpath, "/")) {
                    json.put("parent", baseUri);
                } else {
                    json.put("parent", baseUri + "/path/" + parentNode.getPath());
                }
            }
        }
    }

    private void appendChildren(String baseUri, Node node, JSONObject json, HttpServletRequest request) throws Exception {
        NodeIterator itr = node.getNodes();
        while (itr.hasNext()) {
            Node nextNode = itr.nextNode();
            String name = nextNode.getName();
            if (!name.startsWith("jcr:")) {
                JSONObject item = appendFileOrDir(baseUri, nextNode, json);
                loadSelectedProperties(nextNode, item, request);
            }
        }
    }

    private JSONObject appendFileOrDir(String baseUri, Node node, JSONObject json) throws Exception {
        String uri = baseUri + "/path" + node.getPath();

        JSONObject item = new JSONObject();

        String name = node.getName();
        if (StringUtils.equals(name, "/") || StringUtils.isEmpty(name)) {
            item.put("name", "root");
        } else {
            item.put("name", name);
        }
        item.put("uri", uri);
        item.put("path", node.getPath());

        if (markAsFile(node, item)) {
            json.append("files", item);
        } else {
            json.append("directories", item);
        }
        return item;
    }

}

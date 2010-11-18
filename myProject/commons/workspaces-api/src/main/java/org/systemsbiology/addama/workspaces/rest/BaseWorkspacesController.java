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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public abstract class BaseWorkspacesController extends BaseController {
    private static final Logger log = Logger.getLogger(BaseWorkspacesController.class.getName());

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = getUri(request);
        log.info("workspaces for:" + requestUri);

        JSONObject json = new JSONObject();
        json.put("uri", requestUri);

        String nodePath = getNodePath(requestUri);

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (StringUtils.equalsIgnoreCase("get", request.getMethod())) {
            if (!exists(jcrTemplate, nodePath)) {
                throw new ResourceNotFoundException(nodePath);
            }

            Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(nodePath));
            if (isFileNode(node)) {
                return getFile(node);
            }

            appendAnnotations(node, json);
            appendItems(node, json);

        } else if (StringUtils.equalsIgnoreCase("post", request.getMethod())) {
            if(nodePath.endsWith("delete")){
                int index = nodePath.lastIndexOf("/");
                nodePath = nodePath.substring(0,index);
                deleteNode(json, nodePath, jcrTemplate);
            }
            else{
                Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(nodePath));

                String annotations = ServletRequestUtils.getStringParameter(request, "annotations");
                if (!StringUtils.isEmpty(annotations)) {
                    annotateNode(node, new JSONObject(annotations));
                }

                if (ServletFileUpload.isMultipartContent(request)) {
                    try {
                        ServletFileUpload upload = new ServletFileUpload();
                        FileItemIterator itr = upload.getItemIterator(request);
                        while (itr.hasNext()) {
                            FileItemStream itemStream = itr.next();
                            if (!itemStream.isFormField()) {
                                String filename = itemStream.getName();
                                log.info("storeFile(" + filename + ")");
                                storeFile(node, filename, itemStream.openStream());
                                json.accumulate("file", filename);
                            }
                        }

                        json.put("success", true);
                    } catch (Exception e) {
                        log.warning("saveFiles(): unable to extract content:" + e);
                    }
                }
            }
        }  else if (StringUtils.equalsIgnoreCase("delete", request.getMethod())) {
                deleteNode(json, nodePath, jcrTemplate);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    private void deleteNode(JSONObject json, String nodePath, JcrTemplate jcrTemplate) throws Exception {

        if (!exists(jcrTemplate, nodePath)) {
            throw new ResourceNotFoundException(nodePath);
        }

        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(nodePath));
        node.remove();

        json.put("success", true);
    }

    /*
     * Protected Methods
     */

    protected boolean exists(JcrTemplate jcrTemplate, String nodePath) throws Exception {
        return jcrTemplate.itemExists(nodePath);
    }

    protected abstract ModelAndView getFile(Node node) throws Exception;

    protected abstract void storeFile(Node node, String filename, InputStream inputStream) throws Exception;

}
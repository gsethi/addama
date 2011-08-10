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
package org.systemsbiology.addama.workspaces.jcr.rest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.io.utils.EndlineFixingInputStream;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.InputStreamFileView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.springframework.web.bind.ServletRequestUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.zip;
import static org.systemsbiology.addama.jcr.support.JcrTemplateProvider.getJcrTemplate;
import static org.systemsbiology.addama.workspaces.jcr.util.HttpJCR.*;

/**
 * @author hrovira
 */
public class WorkspacesController extends AbstractController {
    private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = getUri(request);

        JSONObject json = new JSONObject();
        json.put("uri", requestUri);

        String nodePath = getNodePath(requestUri);

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (equalsIgnoreCase("get", request.getMethod())) {
            if (!jcrTemplate.itemExists(nodePath)) {
                throw new ResourceNotFoundException(nodePath);
            }

            Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(nodePath));
            if (node.hasProperty("jcr:data")) {
                return getFile(node);
            }

            appendAnnotations(node, json);
            appendItems(node, json);

        } else if (equalsIgnoreCase("post", request.getMethod())) {
            if (nodePath.endsWith("delete")) {
                nodePath = chomp(nodePath, "/");
                deleteNode(json, nodePath, jcrTemplate);
            } else if (nodePath.endsWith("zip")) {
                zipFiles(request, response, jcrTemplate);
            } else {
                Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(nodePath));

                String annotations = getStringParameter(request, "annotations");
                if (!isEmpty(annotations)) {
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
        } else if (equalsIgnoreCase("delete", request.getMethod())) {
            deleteNode(json, nodePath, jcrTemplate);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
    * Private Methods
    */

    private ModelAndView getFile(Node node) throws Exception {
        Property dataProperty = node.getProperty("jcr:data");

        String filename = node.getName();
        ModelAndView mav = new ModelAndView(new InputStreamFileView());
        mav.addObject("inputStream", dataProperty.getStream());
        mav.addObject("mimeType", super.getServletContext().getMimeType(filename));
        mav.addObject("filename", filename);
        return mav;
    }

    private void storeFile(Node node, String filename, InputStream rawInputStream) throws Exception {
        InputStream inputStream = new EndlineFixingInputStream(rawInputStream);
        if (node.hasNode(filename)) {
            Node fileNode = node.getNode(filename);
            fileNode.setProperty("jcr:data", inputStream);
        } else {
            Node fileNode = node.addNode(filename);
            fileNode.addMixin("mix:referenceable");
            fileNode.setProperty("jcr:mimeType", super.getServletContext().getMimeType(filename));
            fileNode.setProperty("jcr:data", inputStream);
        }
    }

    private void zipFiles(HttpServletRequest request, HttpServletResponse response, JcrTemplate jcrTemplate) throws Exception {
        String name = getRequiredStringParameter(request, "name");

        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();
        for (String fileUri : getRequiredStringParameters(request, "uris")) {
            String nodePath = getNodePath(fileUri);
            Node node = (Node) jcrTemplate.getItem(nodePath);
            Property dataProperty = node.getProperty("jcr:data");
            inputStreamsByName.put(node.getName(), dataProperty.getStream());
        }

        zip(response, name + ".zip", inputStreamsByName);
    }

    private void deleteNode(JSONObject json, String nodePath, JcrTemplate jcrTemplate) throws Exception {
        if (!jcrTemplate.itemExists(nodePath)) {
            throw new ResourceNotFoundException(nodePath);
        }

        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(nodePath));
        node.remove();

        json.put("success", true);
    }

}

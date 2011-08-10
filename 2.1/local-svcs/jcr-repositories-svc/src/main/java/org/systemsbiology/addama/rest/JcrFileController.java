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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;
import org.systemsbiology.addama.jcr.util.AddamaFileAppender;
import org.systemsbiology.addama.jcr.util.NodeUtil;
import org.systemsbiology.addama.rest.views.NodeFileView;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.logging.Logger;

import static org.systemsbiology.addama.jcr.support.JcrTemplateProvider.getJcrTemplate;

/**
 * @author hrovira
 */
@Controller
public class JcrFileController extends AbstractJcrController {
    private static final Logger log = Logger.getLogger(JcrFileController.class.getName());

    private AddamaFileAppender fileAppender;

    public void setFileAppender(AddamaFileAppender fileAppender) {
        this.fileAppender = fileAppender;
    }

    /*
    * Controller Methods
    */

    @RequestMapping(value = "/**/file/**", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String path = "/" + StringUtils.substringAfterLast(request.getRequestURI(), "/file/");
        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (jcrTemplate != null && jcrTemplate.itemExists(path)) {
            Node node = (Node) jcrTemplate.getItem(path);
            if (NodeUtil.isFileNode(node)) {
                return new ModelAndView(new NodeFileView(), "node", node);
            }
        }

        throw new ResourceNotFoundException(path);
    }

    @RequestMapping(value = "/**/file/**", method = RequestMethod.PUT)
    @ModelAttribute
    public ModelAndView createByPut(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String path = "/" + StringUtils.substringAfterLast(request.getRequestURI(), "/file/");
        String uri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(path));
        return doSaveFiles(request, node, uri);
    }

    @RequestMapping(value = "/**/file/**/create", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView createByPost(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String path = "/" + StringUtils.substringBetween(request.getRequestURI(), "/file/", "/create");
        String uri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/create");

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(path));
        return doSaveFiles(request, node, uri);
    }

    @RequestMapping(value = "/**/file/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView updateByPath(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String path = "/" + StringUtils.substringAfterLast(request.getRequestURI(), "/file/");

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (!jcrTemplate.itemExists(path)) {
            throw new ResourceNotFoundException(path);
        }

        Node node = (Node) jcrTemplate.getItem(path);

        String uri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        return doSaveFiles(request, node, uri);
    }

    /*
     * Private Methods
     */

    private ModelAndView doSaveFiles(HttpServletRequest request, Node node, String uri) throws Exception {
        log.info(request.getRequestURI() + "," + node.getPath() + "," + uri);

        JSONObject json = new JSONObject();

        String[] filesSaved = saveFiles(request, node);
        for (String fileSaved : filesSaved) {
            JSONObject fileJson = new JSONObject();
            fileJson.put("uri", uri + "/" + fileSaved);
            fileJson.put("name", fileSaved);
            json.append("items", fileJson);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    private String[] saveFiles(HttpServletRequest request, Node node) throws Exception {
        log.info(request.getRequestURI() + "," + node.getPath());

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            log.warning("saveFiles(): not multipart content");
            return new String[0];
        }

        ArrayList<String> savedFiles = new ArrayList<String>();
        try {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator itr = upload.getItemIterator(request);
            if (!itr.hasNext()) {
                log.info("saveFiles(): no files found");
            }

            while (itr.hasNext()) {
                FileItemStream itemStream = itr.next();
                if (!itemStream.isFormField()) {
                    String filename = itemStream.getName();
                    log.info("saveFiles(): " + filename);

                    fileAppender.appendInputStream(node, itemStream.openStream(), filename);
                    savedFiles.add(filename);
                }
            }
        } catch (Exception e) {
            log.warning("saveFiles(): unable to extract content:" + e);
        }
        return savedFiles.toArray(new String[savedFiles.size()]);
    }

}
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
package org.systemsbiology.addama.workspaces.fs.rest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.InputStreamFileView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.workspaces.fs.callbacks.RootPathsJsonConfigHandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class WorkspacesController implements InitializingBean, ServletContextAware {
    private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

    private final Map<String, String> rootPathsByUri = new HashMap<String, String>();

    private JsonConfig jsonConfig;
    private ServletContext servletContext;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new RootPathsJsonConfigHandler(rootPathsByUri));
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String requestUri = getUri(request);
        log.info(requestUri);

        String nodePath = getNodePath(requestUri);
        return appendItemsAndReturn(nodePath);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView post(HttpServletRequest request) throws Exception {
        String requestUri = getUri(request);
        log.info(requestUri);

        String nodePath = getNodePath(requestUri);
        if (nodePath.endsWith("/delete")) {
            return deleteAndReturn(nodePath);
        }

        JSONObject json = new JSONObject();
        json.put("uri", requestUri);

        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator itr = upload.getItemIterator(request);
                while (itr.hasNext()) {
                    FileItemStream itemStream = itr.next();
                    if (!itemStream.isFormField()) {
                        String filename = itemStream.getName();
                        File f = storeFile(nodePath, filename, itemStream.openStream());
                        json.accumulate("items", getJsonForFile(nodePath, f));
                    }
                }

                json.put("success", true);
            } catch (Exception e) {
                log.warning("unable to extract content:" + e);
            }
            
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        File dir = getLocalFile(nodePath);
        dir.mkdirs();

        json.put("name", dir.getName());
        json.put("label", dir.getName());
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView delete(HttpServletRequest request) throws Exception {
        String requestUri = getUri(request);
        log.info(requestUri);
        String nodePath = getNodePath(requestUri);
        return deleteAndReturn(nodePath);
    }

    /*
    * Private Methods
    */

    private String getUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return StringUtils.substringAfterLast(requestUri, request.getContextPath());
    }

    private String getNodePath(String requestUri) {
        String nodePath = requestUri;
        nodePath = StringUtils.replace(nodePath, "%20", " ");
        nodePath = StringUtils.replace(nodePath, "+", " ");
        return nodePath;
    }

    private ModelAndView appendItemsAndReturn(String nodePath) throws Exception {
        File d = getLocalFile(nodePath);
        log.info(d.getPath() + ":" + d.exists());
        if (!d.exists()) {
            throw new ResourceNotFoundException(nodePath);
        }

        if (d.isDirectory()) {
            JSONObject json = new JSONObject();

            for (File f : d.listFiles()) {
                String uri = nodePath + "/" + f.getName();
                if (f.isFile()) {
                    json.append("items", getJsonForFile(nodePath, f));
                } else {
                    JSONObject item = new JSONObject();
                    item.put("name", f.getName());
                    item.put("uri", uri);
                    item.put("isFile", false);
                    json.append("items", item);
                }
            }

            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        String filename = d.getName();
        ModelAndView mav = new ModelAndView(new InputStreamFileView());
        mav.addObject("inputStream", new FileInputStream(d));
        mav.addObject("mimeType", servletContext.getMimeType(filename));
        mav.addObject("filename", filename);
        return mav;
    }

    private JSONObject getJsonForFile(String nodePath, File d) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", d.getName());
        json.put("uri", nodePath + "/" + d.getName());
        json.put("isFile", true);
        json.put("size", d.length());
        json.put("mimeType", servletContext.getMimeType(d.getName()));
        return json;
    }

    private ModelAndView deleteAndReturn(String nodePath) throws Exception {
        File f = getLocalFile(StringUtils.substringBeforeLast(nodePath, "/delete"));
        if (!f.exists()) {
            throw new ResourceNotFoundException(nodePath);
        }
        f.delete();
        return new ModelAndView(new OkResponseView());
    }

    private File storeFile(String nodePath, String filename, InputStream inputStream) throws Exception {
        log.info(nodePath + "/" + filename);

        File dir = getLocalFile(nodePath);
        dir.mkdirs();

        String filepath = dir.getPath() + "/" + filename;
        pipe(inputStream, new FileOutputStream(filepath, false));
        return new File(filepath);
    }

    private File getLocalFile(String nodePath) throws Exception {
        Map.Entry<String, String> entry = getWorkspaceEntry(nodePath);
        return new File(entry.getValue() + StringUtils.substringAfter(nodePath, entry.getKey()));
    }

    private Map.Entry<String, String> getWorkspaceEntry(String uri) {
        for (Map.Entry<String, String> entry : rootPathsByUri.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry;
            }
        }
        return null;
    }

    private void pipe(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[10000];
        int len;
        while ((len = inputStream.read(buf, 0, 1000)) > 0) {
            outputStream.write(buf, 0, len);
        }
        outputStream.close();
    }
}

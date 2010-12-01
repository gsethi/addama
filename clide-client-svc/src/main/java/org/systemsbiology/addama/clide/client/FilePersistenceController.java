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
package org.systemsbiology.addama.clide.client;

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
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.workspaces.callbacks.RootPathsJsonConfigHandler;

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
public class FilePersistenceController implements InitializingBean, ServletContextAware {
    private static final Logger log = Logger.getLogger(FilePersistenceController.class.getName());

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

    /*
     * Controllers
     */

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String requestUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        log.info(requestUri);

        String nodePath = getNodePath(requestUri);
        return appendItemsAndReturn(nodePath);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView post(HttpServletRequest request) throws Exception {
        String requestUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        log.info(requestUri);

        if (requestUri.endsWith("/delete")) {
            throw new UnsupportedOperationException(requestUri);
        }

        JSONObject json = new JSONObject();
        json.put("uri", requestUri);

        if (ServletFileUpload.isMultipartContent(request)) {
            String nodePath = getNodePath(requestUri);

            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator itr = upload.getItemIterator(request);
            while (itr.hasNext()) {
                FileItemStream itemStream = itr.next();
                if (!itemStream.isFormField()) {
                    String filename = itemStream.getName();
                    log.info("storing:" + filename);
                    File f = storeFile(nodePath, filename, itemStream.openStream());
                    json.append("items", getJsonForFile(nodePath, f));
                }
            }

            json.put("success", true);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
    * Private Methods
    */

    private String getNodePath(String requestUri) {
        String nodePath = requestUri;
        nodePath = StringUtils.replace(nodePath, "%20", " ");
        nodePath = StringUtils.replace(nodePath, "+", " ");
        return nodePath;
    }

    private ModelAndView appendItemsAndReturn(String nodePath) throws Exception {
        File d = getLocalFile(nodePath);
        if (d == null || !d.exists()) {
            throw new ResourceNotFoundException(nodePath);
        }

        if (d.isDirectory()) {
            JSONObject json = new JSONObject();

            for (File f : d.listFiles()) {
                if (f.isFile()) {
                    json.append("items", getJsonForFile(nodePath, f));
                } else {
                    JSONObject item = new JSONObject();
                    item.put("name", f.getName());
                    item.put("uri", nodePath + "/" + f.getName());
                    item.put("isFile", false);
                    json.append("items", item);
                }
            }

            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        } else {
            return new ModelAndView(new JsonView()).addObject("json", getJsonForFile(nodePath, d));
        }
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

    private File storeFile(String nodePath, String filename, InputStream inputStream) throws Exception {
        File dir = getLocalFile(nodePath);
        dir.mkdirs();

        String filepath = dir.getPath() + "/" + filename;
        pipe(inputStream, new FileOutputStream(filepath, false));
        return new File(filepath);
    }

    private File getLocalFile(String uri) throws Exception {
        for (Map.Entry<String, String> entry : rootPathsByUri.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return new File(entry.getValue() + StringUtils.substringAfter(uri, entry.getKey()));
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

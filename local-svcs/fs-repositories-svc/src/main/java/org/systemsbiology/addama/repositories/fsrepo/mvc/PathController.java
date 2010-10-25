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
package org.systemsbiology.addama.repositories.fsrepo.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.registry.JsonConfigHandler;
import org.systemsbiology.addama.repositories.fsrepo.util.RangeHeaderUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class PathController implements InitializingBean {
    private static final Logger log = Logger.getLogger(PathController.class.getName());

    private final HashMap<String, String> repositoryPathsByUri = new HashMap<String, String>();
    private final HashMap<String, Boolean> serveFilesByUri = new HashMap<String, Boolean>();

    private JsonConfig jsonConfig;
    private RangeHeaderUtil rangeHeaderUtil;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void setRangeHeaderUtil(RangeHeaderUtil rangeHeaderUtil) {
        this.rangeHeaderUtil = rangeHeaderUtil;
    }

    /*
     * InitializingBean
     */

    public void afterPropertiesSet() throws Exception {
        log.info("map local paths: start");
        jsonConfig.processConfiguration(new JsonConfigHandler() {
            public void handle(JSONObject configuration) throws Exception {
                if (configuration.has("locals")) {
                    JSONArray locals = configuration.getJSONArray("locals");
                    for (int i = 0; i < locals.length(); i++) {
                        JSONObject local = locals.getJSONObject(i);
                        repositoryPathsByUri.put(local.getString("uri"), local.getString("rootPath"));
                        serveFilesByUri.put(local.getString("uri"), local.optBoolean("serveFiles"));
                    }
                }
            }
        });
        log.info("map local paths: finish");
    }

    /*
    * Controllers
    */

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        log.info("get(" + request.getRequestURI() + ")");

        String repositoryUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
        String path = "";
        if (repositoryUri.contains("/path")) {
            repositoryUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/path");
            path = StringUtils.substringAfter(request.getRequestURI(), "/path");
            if (path.contains("/dir")) {
                path = StringUtils.substringBetween(request.getRequestURI(), "/path", "/dir");
            }
            path = StringUtils.chomp(path, "/");
        }

        repositoryUri = StringUtils.chomp(repositoryUri, "/");

        Resource resource = getTargetResource(repositoryUri, path);
        File file = resource.getFile();
        boolean serveFiles = serveFiles(repositoryUri);

        JSONObject json = new JSONObject();
        json.put("isFile", file.isFile());
        json.put("name", file.getName());
        if (StringUtils.isEmpty(path)) {
            json.put("uri", repositoryUri);
        } else {
            json.put("uri", repositoryUri + "/path" + path);
        }
        setFileUri(file, json, serveFiles);
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                json.append("items", getJson(f, json, serveFiles));
            }
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/dir", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView dir(HttpServletRequest request) throws Exception {
        String repositoryUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/dir");
        String path = "";
        if (repositoryUri.contains("/path")) {
            repositoryUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/path");
            path = StringUtils.substringBetween(request.getRequestURI(), "/path", "/dir");
        }

        Resource resource = getTargetResource(repositoryUri, path);
        File file = resource.getFile();
        boolean serveFiles = serveFiles(repositoryUri);

        String uri = repositoryUri;
        if (!StringUtils.isEmpty(path)) {
            uri = repositoryUri + "/path" + path;
        }

        JSONObject json = new JSONObject();
        json.put("isFile", file.isFile());
        json.put("name", file.getName());
        json.put("uri", uri);
        setFileUri(file, json, serveFiles);
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                JSONObject fJson = getJson(f, json, serveFiles);
                if (f.isFile()) {
                    json.append("files", fJson);
                } else if (f.isDirectory()) {
                    json.append("directories", fJson);
                }
            }
        }

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    @RequestMapping(value = "/**/file/**", method = RequestMethod.GET)
    public void file(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("file(" + request.getRequestURI() + ")");

        String repositoryUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/file/");
        repositoryUri = StringUtils.chomp(repositoryUri, "/");

        if (!serveFilesByUri.containsKey(repositoryUri) || !serveFilesByUri.get(repositoryUri)) {
            throw new ResourceNotFoundException(request.getRequestURI() + ": not authorized to serve this file.  get local path instead");
        }

        String path = StringUtils.substringAfterLast(request.getRequestURI(), "/file/");
        path = "/" + path.replaceAll("%20", " ");

        Resource resource = getTargetResource(repositoryUri, path);
        rangeHeaderUtil.processRequest(request, response, resource);
    }

    /*
     * Private Methods
     */

    private Resource getTargetResource(String repository, String path) throws Exception {
        log.info("getTargetResource(" + repository + "," + path + ")");
        if (!repositoryPathsByUri.containsKey(repository)) {
            throw new ResourceNotFoundException(repository);
        }

        String resourcePath = repositoryPathsByUri.get(repository);
        if (!StringUtils.isEmpty(path)) {
            return new FileSystemResource(resourcePath + path);
        }
        return new FileSystemResource(resourcePath);
    }

    private JSONObject getJson(File file, JSONObject parent, boolean serveFiles) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("isFile", file.isFile());
        json.put("name", file.getName());

        if (parent != null && parent.has("uri")) {
            String parentUri = parent.getString("uri");
            if (StringUtils.contains(parentUri, "/path")) {
                json.put("uri", parentUri + "/" + file.getName());
            } else {
                json.put("uri", parentUri + "/path/" + file.getName());
            }

        }

        setFileUri(file, json, serveFiles);

        return json;
    }

    private void setFileUri(File file, JSONObject json, boolean serveFiles) throws JSONException {
        if (!file.isFile()) {
            return;
        }

        if (serveFiles) {
            if (json.has("uri")) {
                String uri = json.getString("uri");
                if (StringUtils.contains(uri, "/path/")) {
                    json.put("file", StringUtils.replace(uri, "/path/", "/file/"));
                }
            }
        }
        json.put("local", file.getPath());
        // TODO : append operating system specific local paths
    }

    private boolean serveFiles(String repositoryUri) {
        boolean serveFiles = serveFilesByUri.get(repositoryUri);
        log.info("serveFiles(" + repositoryUri + "):" + serveFiles);
        return serveFiles;
    }
}

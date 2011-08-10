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
package org.systemsbiology.addama.fsutils.controllers.repositories;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.FilesDirectoriesJsonView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.fsutils.rest.HttpRepositories.getRepositoryUri;
import static org.systemsbiology.addama.fsutils.rest.HttpRepositories.getResourcePath;
import static org.systemsbiology.addama.fsutils.rest.UriScheme.*;

/**
 * @author hrovira
 */
@Controller
public class ItemsController extends FileSystemController {
    private static final String DIR = "/dir";

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String repositoryUri = getRepositoryUri(request, path);
        String resourcePath = getResourcePath(request, path, DIR);

        String uri = chomp(repositoryUri + "/" + path.name() + resourcePath, "/" + path.name());
        File f = getTargetResource(repositoryUri, resourcePath).getFile();
        boolean serveFiles = allowsServingFiles(repositoryUri);

        JSONObject json = getJson(uri, f, serveFiles);

        if (request.getRequestURI().endsWith(DIR)) {
            appendFiles(json, f, serveFiles);
            return new ModelAndView(new FilesDirectoriesJsonView()).addObject("json", json);
        }

        appendItems(json, f, serveFiles);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private JSONObject getJson(String uri, File f, boolean serveFiles) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("isFile", f.isFile());
        json.put("name", f.getName());

        if (f.isFile()) {
            if (serveFiles) {
                if (contains(uri, path.name())) {
                    json.put(file.name(), replace(uri, path.name(), file.name()));
                    json.put(contents.name(), replace(uri, path.name(), contents.name()));
                    json.put("zip", uri + "/zip");
                }
            } else {
                // TODO : append operating system specific local paths
                json.put("local", f.getPath());
            }
        }
        return json;
    }

    private void appendItems(JSONObject parent, File f, boolean serveFiles) throws JSONException {
        if (f.isDirectory()) {
            for (File subfile : f.listFiles()) {
                String childUri = getSubUri(parent.getString("uri"), subfile);
                parent.append("items", getJson(childUri, subfile, serveFiles));
            }
        }
    }

    private void appendFiles(JSONObject parent, File f, boolean serveFiles) throws JSONException {
        if (f.isDirectory()) {
            for (File subfile : f.listFiles()) {
                String childUri = getSubUri(parent.getString("uri"), subfile);
                JSONObject child = getJson(childUri, subfile, serveFiles);

                if (subfile.isFile()) {
                    parent.append("files", child);
                } else {
                    parent.append("directories", child);
                }
            }
        }
    }

    private String getSubUri(String uri, File f) {
        if (contains(uri, path.name())) {
            return uri + "/" + f.getName();
        }
        return uri + "/" + path.name() + "/" + f.getName();
    }
}

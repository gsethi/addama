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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getSpacedURI;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getURI;
import static org.systemsbiology.addama.fsutils.rest.UriScheme.*;

/**
 * @author hrovira
 */
@Controller
public class ItemsController extends FileSystemController {

    @RequestMapping(value = "/**/repositories/{repositoryId}", method = RequestMethod.GET)
    public ModelAndView get(HttpServletRequest request, @PathVariable("repositoryId") String repositoryId) throws Exception {
        String uri = getURI(request);
        File f = getTargetResource(repositoryId, "").getFile();
        JSONObject json = getJson(uri, f);
        appendItems(json, f);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**", method = RequestMethod.GET)
    public ModelAndView path(HttpServletRequest request, @PathVariable("repositoryId") String repositoryId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, "/path");
        File f = getTargetResource(repositoryId, path).getFile();
        JSONObject json = getJson(uri, f);
        appendItems(json, f);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private JSONObject getJson(String uri, File f) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("isFile", f.isFile());
        json.put("name", f.getName());

        if (f.isFile() && contains(uri, path.name())) {
            json.put(file.name(), replace(uri, path.name(), file.name()));
            json.put(contents.name(), replace(uri, path.name(), contents.name()));
            json.put("zip", uri + "/zip");
        }

        return json;
    }

    private void appendItems(JSONObject parent, File f) throws JSONException {
        if (f.isDirectory()) {
            for (File subfile : f.listFiles()) {
                String childUri = getSubUri(parent.getString("uri"), subfile);
                parent.append("items", getJson(childUri, subfile));
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

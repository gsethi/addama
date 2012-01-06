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
package org.systemsbiology.addama.fsutils.controllers.workspaces;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.commons.web.views.ResourceFileView;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;
import org.systemsbiology.addama.fsutils.util.EndlineFixingInputStream;
import org.systemsbiology.addama.fsutils.util.NotStartsWithFilenameFilter;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;
import static org.systemsbiology.addama.commons.web.views.ResourceFileView.RESOURCE;

/**
 * @author hrovira
 */
@Controller
public class CrudController extends FileSystemController {
    private static final Logger log = Logger.getLogger(CrudController.class.getName());

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = RequestMethod.GET)
    public ModelAndView get(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);

        String path = substringAfterLast(uri, workspaceId);
        Resource resource = getTargetResource(workspaceId, path);

        File resourceFile = resource.getFile();
        if (resourceFile.isDirectory()) {
            JSONObject json = new JSONObject();
            for (File f : resourceFile.listFiles(new NotStartsWithFilenameFilter("."))) {
                json.append("items", fileAsJson(uri + "/" + f.getName(), f, request));
            }
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        return new ModelAndView(new ResourceFileView()).addObject(RESOURCE, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = RequestMethod.POST)
    public ModelAndView post(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, workspaceId);

        if (!isMultipartContent(request)) {
            Resource r = getTargetResource(workspaceId, path);

            File dir = r.getFile();
            dir.mkdirs();

            JSONObject json = new JSONObject();
            json.put("uri", uri);
            json.put("name", dir.getName());
            json.put("label", dir.getName());
            return new ModelAndView(new JsonView()).addObject("json", json);
        }

        JSONObject json = new JSONObject();
        json.put("uri", uri);

        try {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator itr = upload.getItemIterator(request);
            while (itr.hasNext()) {
                FileItemStream itemStream = itr.next();
                if (!itemStream.isFormField()) {
                    String filename = itemStream.getName();
                    if (contains(filename, "\\")) {
                        filename = substringAfterLast(filename, "\\");
                    }

                    Resource r = getTargetResource(workspaceId, path + "/" + filename);
                    store(r, itemStream.openStream());

                    json.append("items", fileAsJson(uri + "/" + filename, r.getFile(), request));
                }
            }

            json.put("success", true);
        } catch (Exception e) {
            log.warning("unable to extract content:" + e);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }


    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/delete", method = RequestMethod.POST)
    public ModelAndView delete_by_post(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/delete");
        return mavDelete(workspaceId, path);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**", method = RequestMethod.DELETE)
    public ModelAndView delete(HttpServletRequest request, @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, workspaceId);
        return mavDelete(workspaceId, path);
    }

    /*
    * Private Methods
    */

    private ModelAndView mavDelete(String workspaceId, String path) throws Exception {
        Resource resource = getTargetResource(workspaceId, path);
        recurseDelete(resource.getFile());
        return new ModelAndView(new OkResponseView());
    }

    private void recurseDelete(File... files) {
        for (File sf : files) {
            if (sf.isDirectory()) {
                recurseDelete(sf.listFiles());
            }
            sf.delete();
        }
    }

    private void store(Resource resource, InputStream inputStream) throws Exception {
        File f = resource.getFile();
        if (!f.exists()) {
            f.getParentFile().mkdirs();
        }

        pipe_close(new EndlineFixingInputStream(inputStream), new FileOutputStream(f.getPath(), false));
    }

    private JSONObject fileAsJson(String uri, File f, HttpServletRequest request) throws JSONException, IOException {
        String filename = f.getName();

        JSONObject json = new JSONObject();
        json.put("name", filename);
        json.put("label", filename);
        json.put("uri", uri);

        boolean isFile = f.isFile();
        json.put("isFile", isFile);
        if (isFile) {
            json.put("size", f.length());
            json.put("mimeType", getMimeType(request, f));
        }
        return json;
    }

}

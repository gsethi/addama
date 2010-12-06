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
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.InputStreamFileView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.workspaces.rest.BaseController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class WorkspacesController extends BaseController {
    private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = getUri(request);
        log.info("workspaces for:" + requestUri);


        String nodePath = getNodePath(requestUri);
        if (StringUtils.equalsIgnoreCase("get", request.getMethod())) {
            return appendItemsAndReturn(nodePath);

        } else if (StringUtils.equalsIgnoreCase("post", request.getMethod())) {
            if (nodePath.endsWith("/delete")) {
                return deleteAndReturn(nodePath);
            } else {
                if (ServletFileUpload.isMultipartContent(request)) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("uri", requestUri);

                        ServletFileUpload upload = new ServletFileUpload();
                        FileItemIterator itr = upload.getItemIterator(request);
                        while (itr.hasNext()) {
                            FileItemStream itemStream = itr.next();
                            if (!itemStream.isFormField()) {
                                String filename = itemStream.getName();
                                log.info("storeFile(" + filename + ")");
                                storeFile(nodePath, filename, itemStream.openStream());
                                json.accumulate("file", filename);
                            }
                        }

                        json.put("success", true);
                        return new ModelAndView(new JsonItemsView()).addObject("json", json);
                    } catch (Exception e) {
                        log.warning("saveFiles(): unable to extract content:" + e);
                    }
                }
            }
        } else if (StringUtils.equalsIgnoreCase("delete", request.getMethod())) {
            return deleteAndReturn(nodePath);
        }

        return null;
    }

    /*
    * Private Methods
    */

    private ModelAndView appendItemsAndReturn(String nodePath) throws Exception {
        File d = getLocalFile(nodePath);
        if (!d.exists()) {
            throw new ResourceNotFoundException(nodePath);
        }

        if (d.isDirectory()) {
            JSONObject json = new JSONObject();

            for (File f : d.listFiles()) {
                String uri = nodePath + "/" + f.getName();
                JSONObject item = new JSONObject();
                item.put("name", f.getName());
                item.put("uri", uri);
                item.put("isFile", f.isFile());
                json.append("items", item);
            }

            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        } else {
            String filename = d.getName();
            ModelAndView mav = new ModelAndView(new InputStreamFileView());
            mav.addObject("inputStream", new FileInputStream(d));
            mav.addObject("mimeType", super.getServletContext().getMimeType(filename));
            mav.addObject("filename", filename);
            return mav;
        }
    }

    private ModelAndView deleteAndReturn(String nodePath) throws Exception {
        File f = getLocalFile(StringUtils.substringBeforeLast(nodePath, "/delete"));

        if (!f.exists()) {
            throw new ResourceNotFoundException(nodePath);
        }
        f.delete();
        return new ModelAndView(new OkResponseView());
    }

    private void storeFile(String nodePath, String filename, InputStream inputStream) throws Exception {
        File dir = getLocalFile(nodePath);
        dir.mkdirs();

        String filepath = dir.getPath() + "/" + filename;
        pipe(inputStream, new FileOutputStream(filepath, false));
    }

    private File getLocalFile(String nodePath) throws Exception {
        String rootPath = getRootPath(nodePath);
        return new File(rootPath + nodePath);
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

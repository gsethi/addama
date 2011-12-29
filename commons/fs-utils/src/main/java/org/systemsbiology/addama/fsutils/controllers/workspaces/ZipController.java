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

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;

/**
 * @author hrovira
 */
@Controller
public class ZipController extends FileSystemController {

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/zip", method = RequestMethod.GET)
    public void zipDir(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("workspaceId") String workspaceId) throws Exception {
        String uri = getSpacedURI(request);
        String path = substringBetween(uri, workspaceId, "/zip");
        Resource resource = getTargetResource(workspaceId, path);
        zip(response, resource);
    }

    @RequestMapping(value = "/**/workspaces/{workspaceId}/**/zip", method = RequestMethod.POST)
    public void zipFiles(HttpServletResponse response, @PathVariable("workspaceId") String workspaceId,
                         @RequestParam("name") String name, @RequestParam("uris") String[] fileUris) throws Exception {
        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();
        for (String fileUri : fileUris) {
            String path = substringAfter(fileUri, workspaceId);
            Resource resource = getTargetResource(workspaceId, path);
            File f = resource.getFile();
            if (f.isDirectory()) {
                collectFiles(inputStreamsByName, f);
            } else {
                inputStreamsByName.put(resource.getFilename(), resource.getInputStream());
            }
        }

        zip(response, name + ".zip", inputStreamsByName);
    }
}

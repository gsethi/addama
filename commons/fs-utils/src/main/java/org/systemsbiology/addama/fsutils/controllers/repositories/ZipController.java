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

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;

/**
 * @author hrovira
 */
@Controller
public class ZipController extends AbstractRepositoriesController {
    private static final Logger log = Logger.getLogger(ZipController.class.getName());

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**/zip", method = RequestMethod.GET)
    public void zipDir(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);

        String uri = getSpacedURI(request);
        String resourcePath = substringBetween(uri, "/path", "/zip");
        Resource r = getTargetResource(repositoryId, resourcePath);
        zip(response, r);
    }

    @RequestMapping(value = "/**/repositories/{repositoryId}/path/**/zip", method = RequestMethod.POST)
    public void zipFiles(HttpServletResponse response,
                         @PathVariable("repositoryId") String repositoryId,
                         @RequestParam("name") String name, @RequestParam("uris") String[] fileUris) throws Exception {
        assertServesFiles(repositoryId);

        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();
        for (String fileUri : fileUris) {
            try {
                String localPath = chomp(substringAfter(fileUri, "/path"), "/");
                Resource resource = getTargetResource(repositoryId, localPath);
                File f = resource.getFile();
                if (f.isDirectory()) {
                    collectFiles(inputStreamsByName, f);
                } else {
                    inputStreamsByName.put(resource.getFilename(), resource.getInputStream());
                }
            } catch (ResourceNotFoundException e) {
                log.warning("skipping:" + e.getMessage());
            }
        }

        zip(response, name + ".zip", inputStreamsByName);
    }
}

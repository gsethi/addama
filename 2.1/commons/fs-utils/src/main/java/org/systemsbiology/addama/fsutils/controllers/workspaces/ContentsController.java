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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.fsutils.controllers.FileSystemController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;

/**
 * @author hrovira
 */
@Controller
public class ContentsController extends FileSystemController {

    @RequestMapping(value = "/**/contents", method = RequestMethod.GET)
    public void contents(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = getCleanUri(request, "/contents");

        Resource resource = getWorkspaceResource(uri);
        if (!resource.exists()) {
            throw new ResourceNotFoundException(uri);
        }

        setContentType(request, response, resource.getFilename());
        pipe(resource.getInputStream(), response.getOutputStream());
    }
}

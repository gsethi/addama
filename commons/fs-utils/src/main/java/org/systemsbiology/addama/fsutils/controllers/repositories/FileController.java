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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getSpacedURI;
import static org.systemsbiology.addama.fsutils.util.RangeHeaderUtil.outputResource;

/**
 * @author hrovira
 */
@Controller
public class FileController extends AbstractRepositoriesController {

    @RequestMapping(value = "/**/repositories/{repositoryId}/file/**", method = RequestMethod.GET)
    public void file(HttpServletRequest request, HttpServletResponse response,
                     @PathVariable("repositoryId") String repositoryId) throws Exception {
        assertServesFiles(repositoryId);

        String uri = getSpacedURI(request);
        String path = substringAfterLast(uri, "/file");
        Resource resource = getTargetResource(repositoryId, path);
        outputResource(request, response, resource);
    }

}

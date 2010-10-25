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
package org.systemsbiology.addama.services.execution.mvc;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class ViewerController extends BaseController {
    private static final Logger log = Logger.getLogger(ViewerController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    public void getViewer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("getViewer(" + request.getRequestURI() + ")");

        String scriptUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
        log.info("getViewer(" + request.getRequestURI() + "): scriptUri=[" + scriptUri + "]");
        if (!viewersByUri.containsKey(scriptUri)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String viewer = viewersByUri.get(scriptUri);
        log.info("getViewer(" + request.getRequestURI() + "): viewer=[" + viewer + "]");
        if (StringUtils.isEmpty(viewer)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.sendRedirect(viewer);
    }
}

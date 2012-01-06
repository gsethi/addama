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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.clientRedirect;

/**
 * @author hrovira
 */
@Controller
public class ViewerController {
    private static final Logger log = Logger.getLogger(ViewerController.class.getName());

    private final Map<String, String> viewersByToolId = new HashMap<String, String>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(viewersByToolId, "viewer"));
    }

    @RequestMapping(value = "/**/tools/{toolId}/ui", method = RequestMethod.GET)
    public void getViewer(HttpServletResponse response, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);

        if (!viewersByToolId.containsKey(toolId)) {
            throw new ResourceNotFoundException(toolId);
        }

        clientRedirect(response, viewersByToolId.get(toolId));
    }
}

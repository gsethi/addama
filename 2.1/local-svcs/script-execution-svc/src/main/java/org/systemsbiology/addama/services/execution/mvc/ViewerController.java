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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.clientRedirect;
import static org.systemsbiology.addama.services.execution.util.HttpJob.getScriptUri;
import static org.systemsbiology.addama.services.execution.util.HttpJob.scriptExists;

/**
 * @author hrovira
 */
@Controller
public class ViewerController {
    private static final Logger log = Logger.getLogger(ViewerController.class.getName());

    private final Map<String, String> viewersByUri = new HashMap<String, String>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new StringMapJsonConfigHandler(viewersByUri, "viewer"));
    }

    @RequestMapping(method = RequestMethod.GET)
    public void getViewer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info(request.getRequestURI());

        String uri = getScriptUri(request, "/ui");
        scriptExists(uri);

        if (!viewersByUri.containsKey(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        clientRedirect(response, viewersByUri.get(uri));
    }
}

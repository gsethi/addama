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

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.CollectIdsMappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.chomp;
import static org.apache.commons.lang.StringUtils.substringAfter;

/**
 * @author hrovira
 */
@Controller
public class RootController {
    private static final Logger log = Logger.getLogger(RootController.class.getName());

    private final HashSet<String> toolIds = new HashSet<String>();
    private final Map<String, String> viewersById = new HashMap<String, String>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new CollectIdsMappingsHandler(toolIds));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(viewersById, "viewer"));
    }

    @RequestMapping(value = "/**/tools", method = RequestMethod.GET)
    public ModelAndView tools(HttpServletRequest request) throws Exception {
        String uri = chomp(substringAfter(request.getRequestURI(), request.getContextPath()), "/");
        log.fine(uri);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        for (String toolId : toolIds) {
            json.append("items", getItem(toolId, uri + "/" + toolId));
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/tools/{toolId}", method = RequestMethod.GET)
    public ModelAndView tool(HttpServletRequest request, @PathVariable("toolId") String toolId) throws Exception {
        log.fine(toolId);
        String uri = chomp(substringAfter(request.getRequestURI(), request.getContextPath()), "/");
        return new ModelAndView(new JsonView()).addObject("json", getItem(toolId, uri));
    }

    /*
     * Private Methods
     */

    private JSONObject getItem(String toolId, String uri) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", toolId);
        json.put("uri", uri);
        json.put("jobs", uri + "/jobs");
        if (viewersById.containsKey(uri)) {
            json.put("ui", uri + "/ui");
        }
        return json;
    }
}

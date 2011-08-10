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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.substringAfter;

/**
 * @author hrovira
 */
@Controller
public class RootController {
    private static final Logger log = Logger.getLogger(RootController.class.getName());

    private final Map<String, String> scriptsByUri = new HashMap<String, String>();
    private final Map<String, String> viewersByUri = new HashMap<String, String>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new StringMapJsonConfigHandler(scriptsByUri, "script"));
        jsonConfig.visit(new StringMapJsonConfigHandler(viewersByUri, "viewer"));
    }

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView root(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String uri = substringAfter(request.getRequestURI(), request.getContextPath());
        log.fine(request.getRequestURI() + ": uri=[" + uri + "]");
        if (equalsIgnoreCase(uri, "/addama/tools")) {
            JSONObject json = new JSONObject();
            for (String scriptUri : scriptsByUri.keySet()) {
                json.append("items", getItem(scriptUri));
            }
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        return new ModelAndView(new JsonView()).addObject("json", getItem(uri));
    }

    /*
     * Private Methods
     */

    private JSONObject getItem(String uri) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("jobs", uri + "/jobs");
        if (viewersByUri.containsKey(uri)) {
            json.put("ui", uri + "/ui");
        }
        return json;
    }
}

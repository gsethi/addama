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
package org.systemsbiology.addama.appengine.rest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.substringAfterLast;

/**
 * @author hrovira
 */
@Controller
public class RootController {
    private static final Logger log = Logger.getLogger(RootController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        log.info("get(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getTopLevelItems(HttpServletRequest request) throws Exception {
        log.info("getTopLevelItems(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());
        addFamilyItem(json, "Datasources", "/addama/datasources");
        addFamilyItem(json, "Applications", "/addama/apps");
        addFamilyItem(json, "Repositories", "/addama/repositories");
        addFamilyItem(json, "Workspaces", "/addama/workspaces");
        addFamilyItem(json, "Tools", "/addama/tools");
        addFamilyItem(json, "Services", "/addama/services");
        addFamilyItem(json, "Indexes", "/addama/indexes");
        addFamilyItem(json, "Chromosomes", "/addama/chromosomes");
        addFamilyItem(json, "Searchable Services", "/addama/searchables");
        addFamilyItem(json, "Feeds", "/addama/feeds");

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private void addFamilyItem(JSONObject parent, String label, String uri) throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", substringAfterLast(uri, "/addama/"));
        json.put("label", label);
        json.put("uri", uri);
        parent.append("items", json);
    }
}
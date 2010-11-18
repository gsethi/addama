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
package org.systemsbiology.addama.coresvcs.gae.controllers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.coresvcs.gae.services.Groups;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class GroupController {
    private static final Logger log = Logger.getLogger(GroupController.class.getName());

    private Groups groups;

    public void setGroups(Groups groups) {
        this.groups = groups;
    }

    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getGroups(HttpServletRequest request) throws Exception {
        log.info("getGroups(" + request.getRequestURI() + ")");

        JSONArray jsonGroups = groups.getAllGroups();
        if (jsonGroups == null) {
            jsonGroups = new JSONArray();
        }

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());
        json.put("groups", jsonGroups);
        json.put("numberOfGroups", jsonGroups.length());

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    @RequestMapping(value = "/groups/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getGroup(HttpServletRequest request) throws Exception {
        log.info("getGroup(" + request.getRequestURI() + ")");

        JSONObject groupJson = groups.getGroup(request.getRequestURI());
        if (groupJson == null) {
            throw new ResourceNotFoundException(request.getRequestURI());
        }

        groupJson.put("numberOfMembers", groupJson.length());
        groupJson.put("uri", request.getRequestURI());

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", groupJson);
        return mav;
    }
}

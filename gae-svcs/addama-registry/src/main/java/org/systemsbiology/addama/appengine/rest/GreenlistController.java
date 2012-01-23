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

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.editors.JSONArrayPropertyEditor;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.systemsbiology.addama.appengine.util.Greenlist.addGreenlistUser;
import static org.systemsbiology.addama.appengine.util.Greenlist.getGreenlist;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;

/**
 * @author aeakin
 */
@Controller
public class GreenlistController {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONArray.class, new JSONArrayPropertyEditor());
    }

    @RequestMapping(value = "/greenlist", method = GET)
    public ModelAndView list(HttpServletRequest request) throws Exception {
        checkAdmin(request);

        JSONObject json = new JSONObject();
        json.put("uri", "/addama/greenlist");
        for (String user : getGreenlist()) {
            json.append("items", new JSONObject().put("id", user).put("uri", "/addama/users/" + user));
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/greenlist/*", method = POST)
    public ModelAndView addUser(HttpServletRequest request) throws Exception {
        checkAdmin(request);

        String email = substringAfterLast(request.getRequestURI(), "greenlist/");

        addGreenlistUser(email);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/greenlist", method = POST)
    public ModelAndView addUsers(HttpServletRequest request, @RequestParam("emails") JSONArray emails) throws Exception {
        checkAdmin(request);

        for (int i = 0; i < emails.length(); i++) {
            addGreenlistUser(emails.getString(i));
        }

        return new ModelAndView(new OkResponseView());
    }

}

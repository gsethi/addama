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

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.coresvcs.gae.services.Users;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class UserController {
    private static final Logger log = Logger.getLogger(UserController.class.getName());

    private Users users;

    public void setUsers(Users users) {
        this.users = users;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getUsers(HttpServletRequest request) throws Exception {
        log.fine("getUsers(" + request.getRequestURI() + ")");

        JSONArray jsonUsers = users.getAllUsers();
        if (jsonUsers == null) {
            jsonUsers = new JSONArray();
        }

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());
        json.put("users", jsonUsers);
        json.put("numberOfUsers", jsonUsers.length());

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    @RequestMapping(value = "/users/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getUser(HttpServletRequest request) throws Exception {
        log.fine("getUser(" + request.getRequestURI() + ")");

        JSONObject json = users.getUser(request.getRequestURI());
        if (json == null) {
            throw new ResourceNotFoundException(request.getRequestURI());
        }
        json.put("uri", request.getRequestURI());

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    @RequestMapping(value = "/users/whoami", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView whoami(HttpServletRequest request) throws Exception {
        log.fine("whoami(" + request.getRequestURI() + ")");

        String url = StringUtils.substringBeforeLast(request.getRequestURL().toString(), request.getRequestURI());
        JSONObject json = users.whoAmI(request.getUserPrincipal(), url);
        if (json == null) {
            throw new ResourceNotFoundException(request.getRequestURI());
        }

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }
}

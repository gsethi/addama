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

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;

/**
 * @author hrovira
 */
@Controller
public class UserController {
    private final UserService userService = getUserService();

    @RequestMapping(value = "/users/whoami", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView whoami(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = substringBeforeLast(request.getRequestURL().toString(), request.getRequestURI());

        if (!userService.isUserLoggedIn()) {
            response.sendRedirect(userService.createLoginURL(url));
            return null;
        }

        User user = userService.getCurrentUser();

        JSONObject json = new JSONObject();
        json.put("email", user.getEmail());
        json.put("name", user.getNickname());
        json.put("isAdmin", userService.isUserAdmin());
        json.put("logoutUrl", userService.createLogoutURL(url));
        json.put("uri", "/addama/users/" + user.getEmail());
        json.put("channel", "/addama/channels/" + user.getEmail());

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

}

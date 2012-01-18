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

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.systemsbiology.addama.appengine.util.Channels.*;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;

/**
 * @author hrovira
 */
@Controller
public class ChannelServiceController {
    @RequestMapping(value = "/channels/mine")
    public void getmine(HttpServletResponse response) throws Exception {
        response.sendRedirect("/addama/channels/" + getCurrentUser().getEmail());
    }

    @RequestMapping(value = "/channels/*", method = RequestMethod.GET)
    public ModelAndView subscribe(HttpServletRequest request) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());
        json.put("token", myChannelToken());
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/channels/*", method = RequestMethod.POST)
    public ModelAndView publish(HttpServletRequest request, @RequestParam("event") String event) throws Exception {
        JSONObject json = new JSONObject(event);
        json.put("uri", request.getRequestURI());
        json.put("published", new DateTime());
        publishMessage(substringAfterLast(request.getRequestURI(), "channels/"), json);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/channels/admins", method = RequestMethod.POST)
    public ModelAndView toAdmins(HttpServletRequest request, @RequestParam("event") String event) throws Exception {
        checkAdmin(request);

        JSONObject json = new JSONObject(event);
        json.put("uri", request.getRequestURI());
        json.put("published", new DateTime());
        publishToAdmins(json);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }
}

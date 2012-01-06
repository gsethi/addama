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

import com.google.appengine.api.users.User;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.pojos.GreenlistEntry;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;
import static org.systemsbiology.addama.appengine.util.Greenlist.*;

/**
 * @author aeakin
 */
@Controller
public class GreenlistController {
    private static final Logger log = Logger.getLogger(GreenlistController.class.getName());

    @RequestMapping(value = "/greenlist", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView list(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());

        for (GreenlistEntry userItem : getGreenlistUsers()) {
            json.append("items", userItem.toJSON());
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);

    }

    @RequestMapping(value = "/greenlist/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView addNewUser(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String accessPath = getStringParameter(request, "uri");
        String userEmail = substringAfterLast(request.getRequestURI(), "/");
        addGreenlistUser(userEmail, accessPath);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/greenlist", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView addLoggedInUser(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        User user = getCurrentUser();
        String accessPath = getStringParameter(request, "uri");
        addGreenlistUser(user.getEmail(), accessPath);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/greenlist/**/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView deleteByPost(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String accessPath = getStringParameter(request, "uri");
        String userEmail = substringAfterLast(substringBefore(request.getRequestURI(), "/delete"), "/");
        deleteGreenlistUser(userEmail, accessPath);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/greenlist/**/grantaccess", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView grantAccessByPost(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String accessPath = getStringParameter(request, "uri");
        String userEmail = substringAfterLast(substringBefore(request.getRequestURI(), "/grantaccess"), "/");
        grantGreenlistAccess(userEmail, accessPath);

        return new ModelAndView(new OkResponseView());
    }
}

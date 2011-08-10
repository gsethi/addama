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
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.coresvcs.gae.pojos.WhiteListEntry;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;
import static org.systemsbiology.addama.appengine.util.WhiteLists.*;

/**
 * @author aeakin
 */
@Controller
public class WhiteListController {
    private static final Logger log = Logger.getLogger(WhiteListController.class.getName());

    @RequestMapping(value = "/whitelist", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getWhiteList(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());

        for (WhiteListEntry userItem : getWhiteListUsers()) {
            json.append("items", userItem.toJSON());
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);

    }

    @RequestMapping(value = "/whitelist/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView addNewUser(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String accessPath = getStringParameter(request, "uri");
        String userEmail = substringAfterLast(request.getRequestURI(), "/");
        addWhiteListUser(userEmail, accessPath);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/whitelist", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView addLoggedInUser(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        User user = getCurrentUser();
        String accessPath = getStringParameter(request, "uri");
        addWhiteListUser(user.getEmail(), accessPath);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/whitelist/**/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView deleteWhiteListEntryByPost(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String accessPath = getStringParameter(request, "uri");
        String userEmail = substringAfterLast(substringBefore(request.getRequestURI(), "/delete"), "/");
        deleteWhiteListUser(userEmail, accessPath);

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "whitelist/**/grantaccess", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView grantAccessByPost(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String accessPath = getStringParameter(request, "uri");
        String userEmail = substringAfterLast(substringBefore(request.getRequestURI(), "/grantaccess"), "/");
        grantWhiteListAccess(userEmail, accessPath);

        return new ModelAndView(new OkResponseView());
    }
}

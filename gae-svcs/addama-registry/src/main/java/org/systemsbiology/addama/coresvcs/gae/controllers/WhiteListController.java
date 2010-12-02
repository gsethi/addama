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

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.coresvcs.gae.services.Users;
import org.systemsbiology.addama.coresvcs.gae.services.WhiteLists;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * @author aeakin
 */
@Controller
public class WhiteListController {
    private static final Logger log = Logger.getLogger(WhiteListController.class.getName());

    private Users users;

    private WhiteLists whiteLists;

    public void setUsers(Users users) {
        this.users = users;
    }

    public void setWhiteLists(WhiteLists whiteLists){
        this.whiteLists = whiteLists;
    }

    @RequestMapping(value = "/whitelist", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getWhiteList(HttpServletRequest request) throws Exception {
        log.info("getWhiteList(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());
        for (String[] userItem : whiteLists.getWhiteListUsers()) {
            JSONObject json2 = new JSONObject();
            json2.put("user", userItem[0]);
            json2.put("hasAccess", userItem[1]);
            json2.put("uri",userItem[2]);
            json.append("items", json2);
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;

    }

    @RequestMapping(value = "/whitelist/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView addNewUser(HttpServletRequest request) throws Exception {
        log.info("addNewUser(" + request.getRequestURI() + ")");

        String accessPath = ServletRequestUtils.getStringParameter(request,"uri");
        String userEmail = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
        whiteLists.addWhiteListUser(userEmail,accessPath);
        return mav(new JSONObject());
    }

    @RequestMapping(value = "/whitelist", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView addLoggedInUser(HttpServletRequest request) throws Exception {
        log.info("addLoggedInUser(" + request.getRequestURI() + ")");

        String accessPath = ServletRequestUtils.getStringParameter(request,"uri");
        whiteLists.addLoggedInWhiteListUser(accessPath);
        return mav(new JSONObject());
    }

    @RequestMapping(value = "/whitelist/**", method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView deleteWhiteListEntry(HttpServletRequest request) throws Exception {
        log.info("deleteWhiteListEntry(" + request.getRequestURI() + ")");

        String accessPath = ServletRequestUtils.getStringParameter(request,"uri");
        String email = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/")+1);
        whiteLists.deleteWhiteListUser(email,accessPath);
        return mav(new JSONObject().put("uri", request.getRequestURI()));
    }

    @RequestMapping(value = "/whitelist/**/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView deleteWhiteListEntryByPost(HttpServletRequest request) throws Exception {
        log.info("deleteWhiteListEntryByPost(" + request.getRequestURI() + ")");

        String accessPath = ServletRequestUtils.getStringParameter(request,"uri");
        String userEmailTemp = StringUtils.substringBefore(request.getRequestURI(), "/delete");
        String userEmail = userEmailTemp.substring(userEmailTemp.lastIndexOf("/")+1);
        whiteLists.deleteWhiteListUser(userEmail, accessPath);
        return mav(new JSONObject().put("uri", request.getRequestURI()));
    }

    @RequestMapping(value = "whitelist/**/grantaccess", method=RequestMethod.POST)
    @ModelAttribute
    public ModelAndView grantAccessByPost(HttpServletRequest request) throws Exception {
        log.info("grantAccessByPost(" + request.getRequestURI() + ")");

        String accessPath = ServletRequestUtils.getStringParameter(request,"uri");
        String userEmailTemp = StringUtils.substringBefore(request.getRequestURI(), "/grantaccess");
        String userEmail = userEmailTemp.substring(userEmailTemp.lastIndexOf("/")+1);
        whiteLists.grantWhiteListAccess(userEmail,accessPath);
        return mav(new JSONObject());
    }

    /*
     * Private Methods
     */

    private ModelAndView mav(JSONObject json) {
        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }
}

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

import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.ApiProxy;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.coresvcs.gae.pojos.ApiKey;
import org.systemsbiology.addama.coresvcs.gae.services.ApiKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class ApiKeyController {
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";
    private static final Logger log = Logger.getLogger(ApiKeyController.class.getName());

    private ApiKeys apiKeys;

    public void setApiKeys(ApiKeys apiKeys) {
        this.apiKeys = apiKeys;
    }

    @RequestMapping(value = "/apikeys", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getApiKeys(HttpServletRequest request) throws Exception {
        log.info("getApiKeys(" + request.getRequestURI() + ")");

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());
        for (ApiKey apiKey : apiKeys.getApiKeys()) {
            json.append("items", apiKey.toJSON());
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;

    }

    @RequestMapping(value = "/apikeys/addama.properties", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getAddamaProperties(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("getAddamaProperties(" + request.getRequestURI() + ")");
        if (!UserServiceFactory.getUserService().isUserAdmin()) {
            throw new ForbiddenAccessException("this functionality is only available to logged-in administrators");
        }

        ApiKey apiKey = getFirstApiKey();

        StringBuilder builder = new StringBuilder();
        builder.append("httpclient.secureHostUrl=https://").append(APPSPOT_HOST);
        builder.append("\n");
        builder.append("httpclient.apikey=");
        builder.append(apiKey.getKey().toString());
        builder.append("\n");

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment;filename=\"addama.properties\"");

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(builder.toString().getBytes());

        return null;
    }

    @RequestMapping(value = "/apikeys/addama.config", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getPythonConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("getPythonConfig(" + request.getRequestURI() + ")");

        ApiKey apiKey = getFirstApiKey();

        StringBuilder builder = new StringBuilder();
        builder.append("[Connection]");
        builder.append("\n");
        builder.append("host=").append(APPSPOT_HOST);
        builder.append("\n");
        builder.append("apikey=");
        builder.append(apiKey.getKey().toString());
        builder.append("\n");

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment;filename=\"addama.config\"");

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(builder.toString().getBytes());

        return null;
    }

    @RequestMapping(value = "/apikeys", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView genApiKey(HttpServletRequest request) throws Exception {
        log.info("genApiKey(" + request.getRequestURI() + ")");

        ApiKey apiKey = apiKeys.generateKey();
        return mav(apiKey.toJSON());
    }

    @RequestMapping(value = "/apikeys/**", method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView deleteApiKey(HttpServletRequest request) throws Exception {
        log.info("deleteApiKey(" + request.getRequestURI() + ")");

        String apiKey = request.getRequestURI();
        apiKeys.deleteApiKey(apiKey);
        return mav(new JSONObject().put("uri", request.getRequestURI()));
    }

    @RequestMapping(value = "/apikeys/**/delete", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView deleteApiKeyByPost(HttpServletRequest request) throws Exception {
        log.info("deleteApiKeyByPost(" + request.getRequestURI() + ")");

        String apiKey = StringUtils.substringBefore(request.getRequestURI(), "/delete");
        apiKeys.deleteApiKey(apiKey);
        return mav(new JSONObject().put("uri", request.getRequestURI()));
    }

    /*
     * Private Methods
     */

    private ModelAndView mav(JSONObject json) {
        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    private ApiKey getFirstApiKey() throws ForbiddenAccessException {
        for (ApiKey ak : apiKeys.getApiKeysForCurrentUser()) {
            if (ak.getKey() != null) {
                return ak;
            }
        }

        return apiKeys.generateKey();
    }
}
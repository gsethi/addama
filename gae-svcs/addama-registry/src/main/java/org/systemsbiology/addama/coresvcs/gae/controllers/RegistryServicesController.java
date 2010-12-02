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
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.coresvcs.gae.services.Registry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class RegistryServicesController {
    private static final Logger log = Logger.getLogger(RegistryServicesController.class.getName());

    private Registry registry;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    @RequestMapping(value = "/registry/services/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView setService(HttpServletRequest request, HttpServletResponse response, @RequestParam("service") String service) throws Exception {
        log.fine("setService(" + request.getRequestURI() + "):" + service);

        String serviceUri = StringUtils.substringAfterLast(request.getRequestURI(), "/registry/services");
        UUID uuid = registry.setRegistryService("/addama/services" + serviceUri, new JSONObject(service));
        response.addHeader("x-addama-registry-key", uuid.toString());

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/registry/services/**", method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView removeService(HttpServletRequest request) throws Exception {
        log.fine("removeService(" + request.getRequestURI() + ")");

        String serviceUri = StringUtils.substringAfterLast(request.getRequestURI(), "/registry/services");
        registry.removeRegistryService("/addama/services" + serviceUri);
        return new ModelAndView(new OkResponseView());
    }

}
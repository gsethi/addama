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
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class RegistryMappingsController {
    private static final Logger log = Logger.getLogger(RegistryMappingsController.class.getName());

    private Registry registry;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    @RequestMapping(value = "/registry/mappings/**", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView setMapping(HttpServletRequest request, @RequestParam("mapping") String mapping) throws Exception {
        log.fine("setMapping(" + request.getRequestURI() + "):" + mapping);

        String mappingUri = StringUtils.substringAfterLast(request.getRequestURI(), "/registry/mappings");
        registry.setRegistryMapping(mappingUri, new JSONObject(mapping));
        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/registry/mappings/**", method = RequestMethod.DELETE)
    @ModelAttribute
    public ModelAndView removeMapping(HttpServletRequest request) throws Exception {
        log.fine("removeMapping(" + request.getRequestURI() + ")");

        String mappingUri = StringUtils.substringAfterLast(request.getRequestURI(), "/registry/mappings");
        registry.removeRegistryMapping(mappingUri);
        return new ModelAndView(new OkResponseView());
    }
}
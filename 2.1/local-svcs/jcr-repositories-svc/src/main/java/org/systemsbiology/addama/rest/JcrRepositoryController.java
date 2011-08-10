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
package org.systemsbiology.addama.rest;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.rest.json.NodeMetaJSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

import static org.systemsbiology.addama.jcr.support.JcrTemplateProvider.getJcrTemplate;

/**
 * @author hrovira
 */
@Controller
public class JcrRepositoryController extends AbstractJcrController {
    private static final Logger log = Logger.getLogger(JcrRepositoryController.class.getName());

    /*
    * Controller Methods
    */

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getRepositoryRoot(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        if (jcrTemplate != null) {
            JSONObject json = new NodeMetaJSONObject(jcrTemplate.getRootNode(), request, dateFormat);
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }
        throw new ResourceNotFoundException(request.getRequestURI());
    }
}
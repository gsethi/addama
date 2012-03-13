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

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.pojos.RegistryMapping;
import org.systemsbiology.addama.appengine.pojos.RegistryService;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.systemsbiology.addama.appengine.util.Registry.*;

/**
 * @author hrovira
 */
@Controller
public class RegistryBrowseController {
    private static final Logger log = Logger.getLogger(RegistryBrowseController.class.getName());

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView browseItems(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());

        HashSet<String> uritracker = new HashSet<String>();
        appendItems(json, getRegistryMappingFamily(request.getRequestURI()), uritracker);
        appendItems(json, getRegistryMappingsByItsUri(request.getRequestURI()), uritracker);

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    private void appendItems(JSONObject json, Iterable<RegistryMapping> uriMappings, Set<String> uritracker) throws JSONException {
        if (json != null && uriMappings != null) {
            for (RegistryMapping rm : uriMappings) {
                String itemUri = rm.getUri();

                JSONObject item = new JSONObject();
                item.put("id", rm.getId());
                item.put("service", rm.getServiceUri());
                item.put("label", rm.getLabel());
                item.put("uri", rm.getUri());

                if (!uritracker.contains(itemUri)) {
                    json.append("items", item);
                    uritracker.add(itemUri);
                }
            }
        }
    }
}
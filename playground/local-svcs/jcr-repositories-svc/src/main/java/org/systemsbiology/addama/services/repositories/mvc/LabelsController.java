/*
 * Copyright (C) 2003-2010 Institute for Systems Biology
 *                          Seattle, Washington, USA.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package org.systemsbiology.addama.services.repositories.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class LabelsController extends BaseController {
    private static final Logger log = Logger.getLogger(LabelsController.class.getName());

    @RequestMapping(value = "/**/labels", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        Node node = getNode(request, "/labels");
        JSONObject responseJson = constructNodeLabelJson(request, "/labels", node);
        
        return new ModelAndView(new JsonView()).addObject("json", responseJson);
    }

    @RequestMapping(value = "/**/labels", method = RequestMethod.POST)
    public ModelAndView post(HttpServletRequest request, @RequestParam("labels") String json) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        Set<String> labels = getNewLabels(new JSONObject(json));

        Node node = getNode(request, "/labels");
        
        // The default behavior is that labels are additive
        if (false == ServletRequestUtils.getBooleanParameter(request, "overwrite", false)) {
            labels.addAll(getExistingLabels(node));
        }
        node.setProperty("labels", labels.toArray(new String[labels.size()]));
        JSONObject responseJson = constructNodeLabelJson(request, "/labels", node);

        return new ModelAndView(new JsonView()).addObject("json", responseJson);
    }

    /*
     * Private Methods
     */
    private JSONObject constructNodeLabelJson(HttpServletRequest request,
    		String uriSuffix,
    		Node node) throws Exception {

    	uriSuffix = (null == uriSuffix) ? "/" : uriSuffix;
    	String baseUri = StringUtils.chomp(StringUtils.substringAfter
    			(request.getRequestURI(),
    					request.getContextPath()),
    					uriSuffix);

    	JSONObject json = new JSONObject();
        json.put("uri", baseUri);
        for (String label : getExistingLabels(node)) {
            json.append("labels", label);
        }

    	return json;
    }
    
    private Set<String> getExistingLabels(Node node) throws RepositoryException {
        HashSet<String> set = new HashSet<String>();
        if (node.hasProperty("labels")) {
            Property labelsProperty = node.getProperty("labels");
            for (Value value : labelsProperty.getValues()) {
                set.add(value.getString());
            }
        }
        return set;
    }

    private Set<String> getNewLabels(JSONObject json) throws JSONException {
        HashSet<String> set = new HashSet<String>();
        JSONArray labels = json.getJSONArray("labels");
        for (int i = 0; i < labels.length(); i++) {
            set.add(labels.getString(i));
        }
        return set;
    }
}

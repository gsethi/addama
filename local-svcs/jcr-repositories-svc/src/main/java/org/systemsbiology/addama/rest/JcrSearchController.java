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

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.views.JsonResultsView;
import org.systemsbiology.addama.jcr.util.XPathBuilder;
import org.systemsbiology.addama.rest.json.CurrentNodeWithProjectionJSONObject;
import org.systemsbiology.addama.rest.util.ResultsCollector;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class JcrSearchController extends AbstractJcrController {
    private static final Logger log = Logger.getLogger(JcrSearchController.class.getName());

    private DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView search(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI() + ":" + request.getParameterMap());

        Map<String, String[]> searchParams = loadSearchParams(request);

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        List<ResultsCollector> collectors = loadResults(jcrTemplate, searchParams);

        boolean matchAll = ServletRequestUtils.getBooleanParameter(request, "MATCHING_ALL_TERMS", false);
        Map<String, Node> nodesByUuid = joinResults(matchAll, collectors);

        JSONObject json = new JSONObject();
        for (Node node : nodesByUuid.values()) {
            json.append("results", new CurrentNodeWithProjectionJSONObject(node, request, dateFormat));
        }

        return new ModelAndView(new JsonResultsView()).addObject("json", json);
    }

    /*
    * Private Methods
    */

    private Map<String, String[]> loadSearchParams(HttpServletRequest request) {
        Map<String, String[]> searchParams = new HashMap<String, String[]>();
        Map requestParams = request.getParameterMap();
        for (Object o : requestParams.keySet()) {
            String key = (String) o;
            if (!StringUtils.equals(key, "PROJECTION") && !StringUtils.equals(key, "MATCHING_ALL_TERMS")) {
                if (StringUtils.equals(key, "q")) {
                    String q = request.getParameter("q");
                    q = StringUtils.replace(q, "%20", " ");
                    q = StringUtils.replace(q, "+", " ");
                    searchParams.put("q", new String[]{q});
                } else {
                    searchParams.put(key, request.getParameterValues(key));
                }
            }
        }
        return searchParams;
    }

    private List<ResultsCollector> loadResults(JcrTemplate jcrTemplate, Map<String, String[]> searchParams) throws Exception {
        List<ResultsCollector> collectors = new ArrayList<ResultsCollector>();
        for (Map.Entry<String, String[]> entry : searchParams.entrySet()) {
            String[] values = entry.getValue();

            List<String> queryStrings = new ArrayList<String>();
            for (String value : values) {
                loadQueryStrings(entry.getKey(), value, queryStrings);
            }

            ResultsCollector collector = new ResultsCollector();
            for (String queryString : queryStrings) {
                collector.addQueryResult(jcrTemplate.query(queryString));
            }

            if (collector.hasResults()) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    private void loadQueryStrings(String key, String value, List<String> queryStrings) throws Exception {
        String propName = ".";
        if (!StringUtils.isEmpty(key) && !"q".equals(key)) {
            propName = XPathBuilder.getISO9075XKey(key);
        }

        queryStrings.add("//*[jcr:contains(" + propName + ", '" + value + "')]");
    }

    private Map<String, Node> joinResults(boolean matchAllTerms, List<ResultsCollector> collectors) {
        Map<String, Node> nodesByUuid = new HashMap<String, Node>();
        if (!collectors.isEmpty()) {
            if (matchAllTerms) {
                // AND: intersect results
                ResultsCollector minCollector = collectors.get(0);
                for (ResultsCollector collector : collectors) {
                    if (minCollector.getNumberOfResults() > collector.getNumberOfResults()) {
                        minCollector = collector;
                    }
                }

                Set<String> minUUIDs = new HashSet<String>(minCollector.getUuids());
                for (ResultsCollector collector : collectors) {
                    minUUIDs.retainAll(collector.getUuids());
                }

                for (String minUUID : minUUIDs) {
                    nodesByUuid.put(minUUID, minCollector.getNodesByUuid().get(minUUID));
                }
            } else {
                // OR : union results
                for (ResultsCollector collector : collectors) {
                    nodesByUuid.putAll(collector.getNodesByUuid());
                }
            }
        }
        return nodesByUuid;
    }
}
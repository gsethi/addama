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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

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

/**
 * The search API exposed by this controller is currently ad hoc but it is
 * moving in the direction of the Google Data Protocol
 * http://code.google.com/apis/gdata/docs/2.0/reference.html#Queries Use the RFC
 * 3339 timestamp format. For example: 2005-08-09T10:57:00-08:00.
 *
 * @author hrovira
 */
@Controller
public class JcrSearchController extends AbstractJcrController {
    private static final Logger log = Logger.getLogger(JcrSearchController.class.getName());

    public static final String UPDATED_MIN_PARAM = "updated-min";
    public static final String MAX_RESULTS_PARAM = "max-results";
    public static final String START_INDEX_PARAM = "start-index";
    public static final int DEFAULT_START_INDEX = 1;
    public static final int DEFAULT_MAX_RESULTS = Integer.MAX_VALUE;

    private DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView search(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI() + ":" + request.getParameterMap());

        Map<String, String[]> searchParams = loadSearchParams(request);

        // Retrieve and remove our pagination parameters from the search query
        int startIndex = searchParams.containsKey(START_INDEX_PARAM)
            ? Integer.valueOf(searchParams.remove(START_INDEX_PARAM)[0])
            : DEFAULT_START_INDEX;
        startIndex = (0 < startIndex) ? startIndex : DEFAULT_START_INDEX;
        int maxResults = searchParams.containsKey(MAX_RESULTS_PARAM)
            ? Integer.valueOf(searchParams.remove(MAX_RESULTS_PARAM)[0])
            : DEFAULT_MAX_RESULTS;
        maxResults = (0 < maxResults) ? maxResults : DEFAULT_MAX_RESULTS;

        JcrTemplate jcrTemplate = getJcrTemplate(request);
        List<ResultsCollector> collectors = loadResults(jcrTemplate, searchParams);

        boolean matchAll = ServletRequestUtils.getBooleanParameter(request, "MATCHING_ALL_TERMS", false);
        Map<String, Node> nodesByUuid = joinResults(matchAll, collectors);

        // Build our paginated result object, using a linear loop because folks
        // generally only look at the first few pages of a search result, TODO
        // figure out a meaningful ordering of these results, if there was any
        // search rank it got lost in the join because a HashMap was used, once
        // we have a meaningful ordering List.sublist can be used to get the
        // slice to return
        int currentIndex = 1;
        int maxIndex = (Integer.MAX_VALUE == maxResults)
            ? maxResults : startIndex + maxResults;
        JSONObject json = new JSONObject();
        for (Node node : nodesByUuid.values()) {
            if (maxIndex <= currentIndex)
                break;
            if (startIndex <= currentIndex) {
                json.append("results", new CurrentNodeWithProjectionJSONObject
                            (node, request, dateFormat));
            }
            currentIndex++;
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

        // Retrieve and remove our updated-min filter from the search query
        String updatedMin = searchParams.containsKey(UPDATED_MIN_PARAM)
            ? searchParams.remove(UPDATED_MIN_PARAM)[0] : null;

        List<ResultsCollector> collectors = new ArrayList<ResultsCollector>();
        for (Map.Entry<String, String[]> entry : searchParams.entrySet()) {
            String[] values = entry.getValue();

            List<String> queryStrings = new ArrayList<String>();
            for (String value : values) {
                loadQueryStrings(entry.getKey(), value, updatedMin, queryStrings);
            }

            ResultsCollector collector = new ResultsCollector();
            for (String queryString : queryStrings) {
                try {
                    collector.addQueryResult(jcrTemplate.query(queryString));
                }
                catch (Exception e) {
                    log.warning("Query exception: " + e + " for query "
                                + queryString + ", msg: "
                                + e.getMessage() + ", trace: ");
                    e.printStackTrace();
                }
                log.fine("Queried: " + queryString + ", numResults: "
                         + collector.getNumberOfResults());
            }

            if (collector.hasResults()) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    private void loadQueryStrings(String key, String value, String updatedMin,
                                  List<String> queryStrings) throws Exception {

        // Dev Note: if we used the mixin [mix:lastModified] this
        // could filter only on the field @jcr:lastModified instead of
        // both @created-at and @last-modified-at
        String updatedMinFilter = (null == updatedMin)
            ? ""
            : " and (@created-at >= xs:dateTime('" + updatedMin + "')"
            + " or @last-modified-at >= xs:dateTime('" + updatedMin
            + "'))";

        String propName = ".";
        if (!StringUtils.isEmpty(key) && !"q".equals(key)) {
            propName = XPathBuilder.getISO9075XKey(key);
            // Exact matches
            queryStrings.add("//*[" + propName + "='" + value + "'"
                    + updatedMinFilter + "]");
        }

        // Free-text search
        queryStrings.add("//*[jcr:contains(" + propName + ", '" + value
                         + "')" + updatedMinFilter + "]");
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
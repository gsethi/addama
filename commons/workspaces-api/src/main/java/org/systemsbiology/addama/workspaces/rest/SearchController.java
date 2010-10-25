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
package org.systemsbiology.addama.workspaces.rest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.views.JsonResultsView;
import org.systemsbiology.addama.jcr.util.XPathBuilder;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class SearchController extends BaseController {
    private static final Logger log = Logger.getLogger(SearchController.class.getName());

    private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = getUri(request);
        log.info("search for:" + requestUri);

        Map<String, String[]> searchParams = getSearchParams(request);

        JcrTemplate jcrTemplate = getJcrTemplate(request);

        JSONObject json = getResults(jcrTemplate, searchParams);

        return new ModelAndView(new JsonResultsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private Map<String, String[]> getSearchParams(HttpServletRequest request) {
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

    private JSONObject getResults(JcrTemplate jcrTemplate, Map<String, String[]> searchParams) throws Exception {
        JSONObject json = new JSONObject();
//        List<ResultsCollector> collectors = new ArrayList<ResultsCollector>();
        for (Map.Entry<String, String[]> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            List<String> queryStrings = new ArrayList<String>();
            for (String value : values) {
                String propName = ".";
                if (!StringUtils.isEmpty(key) && !"q".equals(key)) {
                    propName = XPathBuilder.getISO9075XKey(key);
                }

                queryStrings.add("//*[jcr:contains(" + propName + ", '" + value + "')]");
            }

            for (String queryString : queryStrings) {
                QueryResult queryResult = jcrTemplate.query(queryString);
                NodeIterator itr = queryResult.getNodes();
                while (itr.hasNext()) {
                    Node node = itr.nextNode();
                    String nodeName = node.getName();

                    JSONObject result = new JSONObject();
                    result.put("name", nodeName);
                    result.put("uri", node.getPath());
                    if (isFileNode(node)) {
                        result.put("isFile", true);
                        result.put("mimeType", super.getServletContext().getMimeType(nodeName));
                    }

                    PropertyIterator propItr = node.getProperties();
                    while (propItr.hasNext()) {
                        Property prop = propItr.nextProperty();
                        if (prop.getDefinition().isMultiple()) {
                            appendProperty(json, prop, prop.getValues());
                        } else {
                            appendProperty(json, prop, prop.getValue());
                        }
                    }
                }
            }
        }

        return json;
    }

    private void appendProperty(JSONObject json, Property property, Value... values) throws RepositoryException, JSONException {
        String propertyName = property.getName();

        if (propertyName.startsWith("jcr:")) return;
        for (Value value : values) {
            switch (property.getType()) {
                case PropertyType.STRING:
                    json.accumulate(propertyName, value.getString());
                    break;
                case PropertyType.DATE:
                    json.accumulate(propertyName, dateFormat.format(value.getDate().getTime()));
                    break;
                case PropertyType.DOUBLE:
                    json.accumulate(propertyName, value.getDouble());
                    break;
                case PropertyType.LONG:
                    json.accumulate(propertyName, value.getLong());
                    break;
                case PropertyType.BOOLEAN:
                    json.accumulate(propertyName, value.getBoolean());
                    break;
                default:
                    json.accumulate(propertyName, value.getString());
            }
        }
    }

}

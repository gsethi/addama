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

import com.google.appengine.api.urlfetch.*;
import com.google.apphosting.api.ApiProxy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonResultsView;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryMapping;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;
import org.systemsbiology.addama.coresvcs.gae.pojos.SearchableResponse;
import org.systemsbiology.addama.coresvcs.gae.services.Registry;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline;
import static com.google.appengine.api.urlfetch.HTTPMethod.GET;

/**
 * @author hrovira
 */
@Controller
public class SearchController {
    private static final Logger log = Logger.getLogger(SearchController.class.getName());
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";

    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    private Registry registry;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    @RequestMapping(value = "/searchables", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView searchables(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        JSONObject json = new JSONObject();
        json.put("uri", request.getRequestURI());

        for (RegistryService registryService : registry.getSearchableServices()) {
            JSONObject item = new JSONObject();
            item.put("uri", registryService.getUri());
            item.put("label", registryService.getLabel());
            item.put("name", registryService.getLabel());
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView searchByParam(HttpServletRequest request, @RequestParam("q") String query) throws Exception {
        log.info(request.getRequestURI() + "," + query + ":" + request.getParameterMap());

        ArrayList<SearchableResponse> searchableResponses = new ArrayList<SearchableResponse>();
        for (RegistryService registryService : registry.getSearchableServices()) {
            for (RegistryMapping registryMapping : registry.getRegistryMappings(registryService)) {
                searchableResponses.add(doSearch(registryService, registryMapping, request));
            }
        }

        JSONObject json = new JSONObject();
        for (SearchableResponse searchableResponse : searchableResponses) {
            collectResults(searchableResponse, json);
        }
        return new ModelAndView(new JsonResultsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private SearchableResponse doSearch(RegistryService registryService, RegistryMapping registryMapping, HttpServletRequest request) throws Exception {
        log.fine("doSearch(" + registryService + "," + registryMapping + "," + request.getQueryString() + ")");

        URL getUrl = getUrlWithParams(request, registryService, registryMapping);
        HTTPRequest searchReq = new HTTPRequest(getUrl, GET, withDeadline(20));
        searchReq.setHeader(new HTTPHeader("x-addama-registry-key", registryService.getAccessKey().toString()));
        searchReq.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_HOST));

        log.info("doSearch():" + getUrl);
        Future<HTTPResponse> response = urlFetchService.fetchAsync(searchReq);
        return new SearchableResponse(response, registryService);
    }

    private void collectResults(SearchableResponse response, JSONObject json) {
        RegistryService registryService = response.getSearchable();
        Future<HTTPResponse> futureResponse = response.getFutureResponse();
        try {
            HTTPResponse resp = futureResponse.get();
            int responseCode = resp.getResponseCode();
            log.info("collectResults(" + registryService + "):" + responseCode);
            if (responseCode == 200) {
                JSONObject searchResponse = new JSONObject(resp.getContent());
                if (searchResponse.has("results")) {
                    JSONArray results = searchResponse.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        result.put("source", registryService.getUri());
                        json.append("results", result);
                    }
                }
                json.append("sources", registryService.getUri());
            }
        } catch (Exception e) {
            log.warning("collectResults(" + registryService + "):" + e);
        }
    }

    private URL getUrlWithParams(HttpServletRequest request, RegistryService registryService, RegistryMapping registryMapping) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append(registryService.getUrl().toString());
        builder.append(registryMapping.getUri());
        builder.append("/search");
        builder.append("?").append(request.getQueryString());
        return new URL(builder.toString());
    }
}
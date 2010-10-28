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
package org.systemsbiology.addama.coresvcs.indexes;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.lucene.search.core.HitExtractor;
import org.springmodules.lucene.search.core.LuceneSearchTemplate;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonResultsView;
import org.systemsbiology.addama.coresvcs.indexes.extractors.JSONObjectExtractor;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class IndexesController extends BaseIndexingController {
    private static final Logger log = Logger.getLogger(IndexesController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getIndex(HttpServletRequest request) throws Exception {
        log.info("getIndex(" + request.getRequestURI() + ")");

        LuceneSearchTemplate searchTemplate = getLuceneSearchTemplate(request);

        String uri = getIndexedUri(request);

        JSONObject json = new JSONObject();
        json.put("uri", uri);

        appendOwnProperties(searchTemplate, uri, json);

        JSONObject[] items = searchByTerm(searchTemplate, "paths", uri);
        log.info("getIndex(" + uri + "): items=" + items.length);
        for (JSONObject item : items) {
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "**/attachments", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getAttachments(HttpServletRequest request) throws Exception {
        log.info("getAttachments(" + request.getRequestURI() + ")");

        LuceneSearchTemplate searchTemplate = getLuceneSearchTemplate(request);

        String uri = StringUtils.substringBeforeLast(getIndexedUri(request), "/attachments");

        JSONObject json = new JSONObject();
        json.put("uri", uri);

        String[] attachments = getAttachments(searchTemplate, "uri", uri);
        log.info("getAttachments(" + uri + "): attachments=" + attachments.length);
        for (String attachment : attachments) {
            json.append("items", attachment);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "**/search", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView searchByParam(HttpServletRequest request, @RequestParam("q") String query) throws Exception {
        log.info("searchByParam(" + request.getRequestURI() + "," + query + "):" + request.getParameterMap());

        LuceneSearchTemplate searchTemplate = getLuceneSearchTemplate(request);

        query = StringUtils.replace(query, "%20", " ");
        query = StringUtils.replace(query, "+", " ");

        JSONObject json = new JSONObject();
        for (JSONObject obj : searchByTerm(searchTemplate, "keywords", query)) {
            json.append("results", obj);
        }

        return new ModelAndView(new JsonResultsView()).addObject("json", json);
    }

    /*
    * Private Methods
    */

    private String getIndexedUri(HttpServletRequest request) {
        String uri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath() + "/addama/indexes");
        if (uri != null) {
            uri = StringUtils.replace(uri, "%20", " ");
            uri = StringUtils.replace(uri, "+", " ");
        }
        if (StringUtils.isEmpty(uri)) {
            return "/";
        }
        return uri;
    }

    private void appendOwnProperties(LuceneSearchTemplate searchTemplate, String uri, JSONObject json) throws JSONException {
        for (JSONObject jsonItem : searchByTerm(searchTemplate, "uri", uri)) {
            Iterator keys = jsonItem.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                json.put(key, jsonItem.get(key));
            }
        }
    }

    private JSONObject[] searchByTerm(LuceneSearchTemplate searchTemplate, String key, String value) throws JSONException {
        Term term = new Term(key, value);
        List items = searchTemplate.search(new TermQuery(term), new JSONObjectExtractor());

        HashMap<String, JSONObject> mergedByUri = new HashMap<String, JSONObject>();
        for (Object obj : items) {
            JSONObject json = (JSONObject) obj;
            if (json.has("uri")) {
                String uri = json.getString("uri");
                if (mergedByUri.containsKey(uri)) {
                    JSONObject merged = merge(mergedByUri.get(uri), json);
                    mergedByUri.put(uri, merged);
                } else {
                    mergedByUri.put(uri, json);
                }
            }
        }
        return mergedByUri.values().toArray(new JSONObject[mergedByUri.values().size()]);
    }

    private String[] getAttachments(LuceneSearchTemplate searchTemplate, String key, String value) throws JSONException {
        Term term = new Term(key, value);
        List items = searchTemplate.search(new TermQuery(term), new HitExtractor() {
            public Object mapHit(int i, Document document, float v) {
                return document.getField("attachments");
            }
        });

        HashSet<String> attachments = new HashSet<String>();
        for (Object item : items) {
            if (item != null && StringUtils.isEmpty(item.toString())) {
                attachments.add(item.toString());
            }
        }
        return attachments.toArray(new String[attachments.size()]);
    }

    private JSONObject merge(JSONObject jsonA, JSONObject jsonB) throws JSONException {
        JSONObject merge = new JSONObject();

        Map<String, Set<Object>> valuemap = new HashMap<String, Set<Object>>();
        transferObjects(jsonA, valuemap);
        transferObjects(jsonB, valuemap);

        for (Map.Entry<String, Set<Object>> entry : valuemap.entrySet()) {
            String key = entry.getKey();
            Set<Object> values = entry.getValue();
            for (Object value : values) {
                merge.accumulate(key, value);
            }
        }

        return merge;
    }

    private void transferObjects(JSONObject json, Map<String, Set<Object>> valuemap) throws JSONException {
        Iterator itr = json.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            Set<Object> objs = valuemap.get(key);
            if (objs == null) {
                objs = new HashSet<Object>();
                valuemap.put(key, objs);
            }

            Object val = json.get(key);
            if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                for (int i = 0; i < arr.length(); i++) {
                    objs.add(arr.get(i));
                }
            } else {
                objs.add(val);
            }
        }
    }
}

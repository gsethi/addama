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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.MemcacheService;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.callbacks.FeedItemsMemcacheLoaderCallback;
import org.systemsbiology.addama.appengine.callbacks.FeedsMemcacheLoaderCallback;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.editors.JSONObjectPropertyEditor;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.RssView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserEmail;
import static org.systemsbiology.addama.appengine.Appspot.APPSPOT_ID;
import static org.systemsbiology.addama.appengine.Appspot.APPSPOT_URL;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServicePaginatedTemplate.loadIfNotExist;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServicePaginatedTemplate.namespacedCache;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServiceTemplate.loadIfNotExisting;
import static org.systemsbiology.addama.commons.web.views.RssView.*;

/**
 * An RSS 2.0 and generic Json items feed service.
 * <p/>
 * RSS 2.0 pagination API per http://tools.ietf.org/html/rfc5005#section-3
 * <p/>
 * For feed item idempotent additions or item update instead of insertion, specify a unique key for the item.
 *
 * @author hrovira
 */
@Controller
public class FeedsController {
    private static final Logger log = Logger.getLogger(FeedsController.class.getName());

    private final MemcacheService feedscache = getMemcacheService("feeds");
    private final DatastoreService datastore = getDatastoreService();

    public static final int PAGE_SIZE = 10;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(JSONObject.class, new JSONObjectPropertyEditor());
    }

    @RequestMapping(value = "/**/feeds", method = RequestMethod.GET)
    public ModelAndView list(HttpServletRequest request) throws Exception {
        String items = (String) loadIfNotExisting(feedscache, "/addama/feeds", new FeedsMemcacheLoaderCallback());
        JSONObject json = new JSONObject(items);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/feeds/{feedId}", method = RequestMethod.GET)
    public ModelAndView feed(@PathVariable("feedId") String feedId,
                             @RequestParam(value = "page", defaultValue = "1") Integer page) throws Exception {
        log.info(feedId + ":" + page);

        if (page <= 0) page = 1;
        int offset = PAGE_SIZE * (page - 1);

        JSONObject json = getItems(feedId, offset);
        json.put("uri", "/addama/feeds/" + feedId + "?page=" + page);
        json.put("rss", "/addama/feeds/" + feedId + "/rss?page=" + page);
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/feeds/{feedId}/rss", method = RequestMethod.GET)
    public ModelAndView rss(@PathVariable("feedId") String feedId,
                            @RequestParam(value = "page", defaultValue = "1") Integer page) throws Exception {
        log.info(feedId + ":" + page);

        if (page <= 0) page = 1;
        int offset = PAGE_SIZE * (page - 1);

        ModelAndView mav = new ModelAndView(new RssView());
        mav.addObject("json", getItems(feedId, offset));
        mav.addObject(FEED_ID, feedId);
        mav.addObject(PAGE_NUMBER, page);
        mav.addObject(APPLICATION_ID, APPSPOT_ID());
        mav.addObject(APPLICATION_URL, APPSPOT_URL());
        return mav;
    }

    @RequestMapping(value = "/**/feeds/{feedId}", method = RequestMethod.POST)
    public ModelAndView publish(HttpServletRequest request, @PathVariable("feedId") String feedId, @RequestParam("item") JSONObject item) throws Exception {
        log.info(feedId + ":" + item);

        String userEmail = getLoggedInUserEmail(request);
        JSONObject json = persistItem(feedId, userEmail, item);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private JSONObject getItems(String feedId, int offset) throws Exception {
        String items = (String) loadIfNotExist(feedId, PAGE_SIZE, offset, new FeedItemsMemcacheLoaderCallback());
        return new JSONObject(items);
    }

    private JSONObject persistItem(String feedId, String userEmail, JSONObject json) throws Exception {
        Key parentKey = createKey("feeds", feedId);
        Entity feedParent;
        try {
            feedParent = datastore.get(parentKey);
        } catch (EntityNotFoundException ex) {
            log.info("creating new feed:" + ex.getMessage());
            feedParent = new Entity(parentKey);
            feedParent.setProperty("creator", userEmail);
        }

        Key k = createKey(parentKey, "feed-item", json.optString("key", randomUUID().toString()));
        // Allow for idempotent additions and updates to feeds

        Entity feedItem = new Entity(k);
        feedItem.setProperty("text", json.getString("text"));

        // Optional properties with default values
        if (json.has("date")) {
            // Dev Note: dates must be specified in a format that is compatible with the ISO8601 standard.
            DateTime theDate = new DateTime(json.getString("date"));
            feedItem.setProperty("date", theDate.toDate());
        } else {
            feedItem.setProperty("date", new Date());
        }

        feedItem.setProperty("author", json.optString("author", userEmail));

        // Optional properties
        if (json.has("title")) {
            feedItem.setProperty("title", json.getString("title"));
        }
        if (json.has("link")) {
            feedItem.setProperty("link", json.getString("link"));
        }

        inTransaction(datastore, new PutEntityTransactionCallback(asList(feedParent, feedItem)));
        namespacedCache(feedId).clearAll();

        return json;
    }
}

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
package org.systemsbiology.addama.gaesvcs.feeds.mvc;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.MemcacheService;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.gaesvcs.feeds.callbacks.FeedItemsMemcacheLoaderCallback;
import org.systemsbiology.addama.gaesvcs.feeds.callbacks.FeedsMemcacheLoaderCallback;
import org.systemsbiology.addama.gaesvcs.feeds.mvc.view.RssView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServicePaginatedTemplate.loadIfNotExist;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServicePaginatedTemplate.namespacedCache;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate.clearMemcache;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate.loadIfNotExisting;

/**
 * An RSS 2.0 and generic Json items feed service.
 * <p/>
 * RSS 2.0 pagination API per http://tools.ietf.org/html/rfc5005#section-3
 * <p/>
 * For feed item idempotent additions or item update instead of insertion, specify a unique key for the item.
 *
 * @author hrovira
 */
public class FeedsController extends AbstractController {
    private static final Logger log = Logger.getLogger(FeedsController.class.getName());

    private final MemcacheService feedscache = getMemcacheService("feeds");
    private final DatastoreService datastore = getDatastoreService();

    public static final String PAGE_PARAM = "page";
    public static final int DEFAULT_PAGE = 1;
    public static final int PAGE_SIZE = 10;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Pagination
        String page = request.getParameter("page");
        int pageNum = (null == page) ? DEFAULT_PAGE : Integer.valueOf(page);
        pageNum = (0 < pageNum) ? pageNum : DEFAULT_PAGE;
        int offset = PAGE_SIZE * (pageNum - 1);

        log.info(method + ":" + requestUri);

        if (StringUtils.equalsIgnoreCase(method, "get")) {
            if (requestUri.endsWith("/rss")) {
                requestUri = StringUtils.substringBeforeLast(requestUri, "/rss");
                JSONObject json = getItems(requestUri, PAGE_SIZE, offset);
                return new ModelAndView(new RssView(requestUri, pageNum)).addObject("json", json);
            } else if (StringUtils.equalsIgnoreCase(requestUri, "/addama/feeds")) {
                JSONObject json = getFeeds();
                return new ModelAndView(new JsonItemsView()).addObject("json", json);
            } else {
                JSONObject json = getItems(requestUri, PAGE_SIZE, offset);
                json.put("uri", requestUri + "?" + PAGE_PARAM + "=" + pageNum);
                json.put("rss", requestUri + "/rss?" + PAGE_PARAM + "=" + pageNum);
                return new ModelAndView(new JsonItemsView()).addObject("json", json);
            }
        } else if (StringUtils.equalsIgnoreCase(method, "post")) {
            JSONObject item = persistItem(request);
            return new ModelAndView(new JsonView()).addObject("json", item);
        }

        return null;
    }

    /*
     * Private Methods
     */

    private JSONObject getFeeds() throws Exception {
        String items = (String) loadIfNotExisting(feedscache, "/addama/feeds", new FeedsMemcacheLoaderCallback());
        return new JSONObject(items);
    }

    private JSONObject getItems(String feedUri, int limit, int offset) throws Exception {
        String items = (String) loadIfNotExist(feedUri, limit, offset, new FeedItemsMemcacheLoaderCallback());
        return new JSONObject(items);
    }

    private JSONObject persistItem(HttpServletRequest request) throws Exception {
        JSONObject json = new JSONObject(ServletRequestUtils.getRequiredStringParameter(request, "item"));

        String feedUri = request.getRequestURI();
        String userUri = request.getHeader("x-addama-registry-user");

        Entity p = getFeed(feedUri, userUri);
        Key k;
        // Allow for idempotent additions and updates to feeds
        if (json.has("key")) {
            k = KeyFactory.createKey(p.getKey(), "feed-item", json.getString("key"));
        } else {
            k = KeyFactory.createKey(p.getKey(), "feed-item", UUID.randomUUID().toString());
        }
        Entity e = new Entity(k);

        // The only required property is 'text'
        e.setProperty("text", json.getString("text"));

        // Optional properties with default values
        if (json.has("date")) {
            // Dev Note: dates must be specified in a format that is compatible with the ISO8601 standard.
            DateTime theDate = new DateTime(json.getString("date"));
            e.setProperty("date", theDate.toDate());
        } else {
            e.setProperty("date", new Date());
        }
        if (json.has("author")) {
            e.setProperty("author", json.getString("author"));
        } else {
            e.setProperty("author", userUri);
        }

        // Optional properties
        if (json.has("title")) {
            e.setProperty("title", json.getString("title"));
        }
        if (json.has("link")) {
            e.setProperty("link", json.getString("link"));
        }

        inTransaction(datastore, new PutEntityTransactionCallback(e));
        namespacedCache(feedUri).clearAll();

        return json;
    }

    private Entity getFeed(String feedUri, String userUri) {
        Key k = KeyFactory.createKey("feeds", feedUri);
        try {
            return datastore.get(k);
        } catch (EntityNotFoundException e1) {
            Entity e = new Entity(k);
            e.setProperty("creator", userUri);
            inTransaction(datastore, new PutEntityTransactionCallback(e));
            clearMemcache(feedscache, "/addama/feeds");
            return e;
        }
    }
}

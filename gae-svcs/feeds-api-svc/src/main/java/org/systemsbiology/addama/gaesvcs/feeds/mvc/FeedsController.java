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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate;
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

/**
 * @author hrovira
 */
public class FeedsController extends AbstractController {
    private static final Logger log = Logger.getLogger(FeedsController.class.getName());

    private MemcacheServiceTemplate memcacheServiceTemplate = new MemcacheServiceTemplate();
    private DatastoreServiceTemplate datastoreServiceTemplate = new DatastoreServiceTemplate();

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        log.info(method + ":" + requestUri);

        if (StringUtils.equalsIgnoreCase(method, "get")) {
            if (requestUri.endsWith("/rss")) {
                requestUri = StringUtils.substringBeforeLast(requestUri, "/rss");
                JSONObject json = getItems(requestUri);
                return new ModelAndView(new RssView()).addObject("json", json);
            } else if (StringUtils.equalsIgnoreCase(requestUri, "/addama/feeds")) {
                JSONObject json = getFeeds();
                return new ModelAndView(new JsonItemsView()).addObject("json", json);
            } else {
                JSONObject json = getItems(requestUri);
                json.put("uri", requestUri);
                json.put("rss", requestUri + "/rss");
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
        String items = (String) memcacheServiceTemplate.loadIfNotExisting("/addama/feeds", new FeedsMemcacheLoaderCallback());
        return new JSONObject(items);
    }

    private JSONObject getItems(String feedUri) throws Exception {
        String items = (String) memcacheServiceTemplate.loadIfNotExisting(feedUri, new FeedItemsMemcacheLoaderCallback());
        return new JSONObject(items);
    }

    private JSONObject persistItem(HttpServletRequest request) throws Exception {
        JSONObject json = new JSONObject(ServletRequestUtils.getRequiredStringParameter(request, "item"));

        String feedUri = request.getRequestURI();
        String userUri = request.getHeader("x-addama-registry-user");

        Entity p = getFeed(feedUri, userUri);
        Key k = KeyFactory.createKey(p.getKey(), "feed-item", UUID.randomUUID().toString());
        Entity e = new Entity(k);
        e.setProperty("text", json.getString("text"));
        if (json.has("date")) {
            e.setProperty("date", json.getString("date"));
        } else {
            e.setProperty("date", new Date());
        }
        if (json.has("author")) {
            e.setProperty("author", json.getString("author"));
        } else {
            e.setProperty("author", userUri);
        }

        datastoreServiceTemplate.inTransaction(new PutEntityTransactionCallback(e));
        memcacheServiceTemplate.clearMemcache(feedUri);

        return json;
    }

    private Entity getFeed(String feedUri, String userUri) {
        Key k = KeyFactory.createKey("feeds", feedUri);
        try {
            return datastoreServiceTemplate.getEntityByKey(k);
        } catch (EntityNotFoundException e1) {
            Entity e = new Entity(k);
            e.setProperty("creator", userUri);
            datastoreServiceTemplate.inTransaction(new PutEntityTransactionCallback(e));
            memcacheServiceTemplate.clearMemcache("/addama/feeds");
            return e;
        }
    }
}

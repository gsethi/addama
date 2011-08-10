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

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.datastore.*;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.channel.ChannelServiceFactory.getChannelService;
import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate.loadIfNotExisting;

/**
 * @author hrovira
 */
@Controller
public class ChannelServiceController implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(ChannelServiceController.class.getName());

    private final DatastoreService datastore = getDatastoreService();

    @RequestMapping(value = "/channels", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getall(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        JSONObject channels = new JSONObject();
        PreparedQuery pq = datastore.prepare(new Query("channels"));
        Iterator<Entity> itr = pq.asIterator();
        while (itr.hasNext()) {
            Entity e = itr.next();
            String uri = StringUtils.chomp(e.getKey().getName(), "/");
            String name = StringUtils.substringAfterLast(uri, "/");
            channels.append("items", new JSONObject().put("uri", uri).put("name", name));
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", channels);
    }

    @RequestMapping(value = "/channels/mine")
    public void getmine(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info(request.getRequestURI());

        response.sendRedirect("/addama/channels/" + getCurrentUser().getEmail());
    }

    @RequestMapping(value = "/channels/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView get(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        JSONObject json = new JSONObject();
        json.put("uri", requestUri);
        json.put("token", getChannelService().createChannel(getChannelKey(requestUri)));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/channels/*", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView post(HttpServletRequest request, @RequestParam("event") String event) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        JSONObject json = new JSONObject(event);
        json.put("uri", requestUri);
        json.put("published", new DateTime());

        getChannelService().sendMessage(new ChannelMessage(getChannelKey(requestUri), json.toString()));

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    /*
     * MemcacheLoaderCallback
     */

    public Serializable getCacheableObject(String key) throws Exception {
        Key k = createKey("channels", key);

        try {
            Entity e = datastore.get(k);
            return e.getProperty("key").toString();
        } catch (EntityNotFoundException e) {
            log.warning("create new key for " + key + ":" + e.getMessage());
        }

        String channelKey = UUID.randomUUID().toString();

        Entity e = new Entity(k);
        e.setProperty("key", channelKey);
        inTransaction(datastore, new PutEntityTransactionCallback(e));
        return channelKey;
    }

    /*
     * Private Methods
     */

    private String getChannelKey(String requestUri) throws Exception {
        return (String) loadIfNotExisting(getMemcacheService("channelkeys"), requestUri, this, byDeltaSeconds(3600));
    }

}

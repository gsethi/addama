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
package org.systemsbiology.addama.gaesvcs.channels.mvc;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.gaesvcs.channels.callbacks.ChannelKeyMemcacheLoaderCallback;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate.loadIfNotExisting;

/**
 * @author hrovira
 */
public class ChannelServiceController extends AbstractController {
    private static final Logger log = Logger.getLogger(ChannelServiceController.class.getName());

    private final ChannelService channelService = ChannelServiceFactory.getChannelService();
    private final MemcacheService channelKeysCache = MemcacheServiceFactory.getMemcacheService("memcache-channelkey");
    private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    /*
    * Handler
    */

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        log.info(method + ":" + requestUri);

        if (StringUtils.equalsIgnoreCase(method, "get")) {
            if (StringUtils.equalsIgnoreCase(requestUri, "/addama/channels")) {
                log.info("retrieving all channels");

                JSONObject channels = new JSONObject();
                PreparedQuery pq = datastoreService.prepare(new Query("channels"));
                Iterator<Entity> itr = pq.asIterator();
                while (itr.hasNext()) {
                    Entity e = itr.next();
                    String uri = StringUtils.chomp(e.getKey().getName(), "/");
                    String name = StringUtils.substringAfterLast(uri, "/");
                    channels.append("items", new JSONObject().put("uri", uri).put("name", name));
                }

                return new ModelAndView(new JsonItemsView()).addObject("json", channels);
            }

            log.info("retrieving channel token");
            JSONObject json = new JSONObject();
            json.put("uri", requestUri);
            json.put("token", channelService.createChannel(getChannelKey(requestUri)));
            return new ModelAndView(new JsonView()).addObject("json", json);
        }

        if (StringUtils.equalsIgnoreCase(method, "post")) {
            log.info("publishing event");

            JSONObject json = new JSONObject(ServletRequestUtils.getRequiredStringParameter(request, "event"));
            json.put("uri", requestUri);
            json.put("published", new DateTime());

            channelService.sendMessage(new ChannelMessage(getChannelKey(requestUri), json.toString()));

            return new ModelAndView(new JsonView()).addObject("json", json);
        }

        return null;
    }

    /*
     * Private Methods
     */

    private String getChannelKey(String requestUri) throws Exception {
        ChannelKeyMemcacheLoaderCallback callback = new ChannelKeyMemcacheLoaderCallback(datastoreService);
        return (String) loadIfNotExisting(channelKeysCache, requestUri, callback, byDeltaSeconds(60));
    }

}

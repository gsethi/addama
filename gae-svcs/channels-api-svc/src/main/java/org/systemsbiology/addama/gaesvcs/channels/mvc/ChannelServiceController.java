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
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ChannelServiceController extends AbstractController {
    private static final Logger log = Logger.getLogger(ChannelServiceController.class.getName());

    private final DatastoreServiceTemplate datastoreServiceTemplate = new DatastoreServiceTemplate();
    private final MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(getClass().getName());
    private final ChannelService channelService = ChannelServiceFactory.getChannelService();

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        log.info(method + ":" + requestUri);

        String channelId = getChannelId(requestUri);

        if (StringUtils.equalsIgnoreCase(method, "get")) {
            JSONObject json = new JSONObject();
            json.put("uri", requestUri);
            json.put("channel_id", channelId);

            return new ModelAndView(new JsonView()).addObject("json", json);
        } else if (StringUtils.equalsIgnoreCase(method, "post")) {
            JSONObject json = new JSONObject(ServletRequestUtils.getRequiredStringParameter(request, "event"));
            json.put("uri", requestUri);

            channelService.sendMessage(new ChannelMessage(channelId, json.toString()));
            return new ModelAndView(new JsonView()).addObject("json", json);
        }

        return null;
    }

    /*
     * Private Methods
     */

    private String getChannelId(String requestUri) {
        if (memcacheService.contains(requestUri)) {
            return (String) memcacheService.get(requestUri);
        }

        Key k = KeyFactory.createKey("channels", requestUri);
        try {
            Entity e = datastoreServiceTemplate.getEntityByKey(k);
            String channelId = e.getProperty("channel").toString();
            memcacheService.put(requestUri, channelId);
            return channelId;
        } catch (EntityNotFoundException e) {
            log.warning(requestUri + ":" + e);
        }

        // TODO : Prepare channels based on what?
        String channelId = channelService.createChannel(requestUri);
        Entity e = new Entity(k);
        e.setProperty("channel", channelId);
        datastoreServiceTemplate.inTransaction(new PutEntityTransactionCallback(e));

        memcacheService.put(requestUri, channelId);
        return channelId;
    }
}

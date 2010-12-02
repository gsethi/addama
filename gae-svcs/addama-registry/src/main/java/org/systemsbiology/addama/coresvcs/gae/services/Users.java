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
package org.systemsbiology.addama.coresvcs.gae.services;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.coresvcs.gae.pojos.CachedUrl;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;

import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class Users {
    private static final Logger log = Logger.getLogger(Users.class.getName());

    private DatastoreServiceTemplate template;

    private MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(getClass().getName());

    public void setTemplate(DatastoreServiceTemplate template) {
        this.template = template;
    }

    public String rememberUser(User user) {
        String userUri = "/addama/users/" + user.getEmail();
        log.info("rememberUser(" + user.getNickname() + "):" + userUri);

        // TODO : Locate user, store if not yet stored
        return userUri;
    }

    public JSONArray getAllUsers() {
        return new JSONArray();
    }

    public JSONObject getUser(String userUri) {
        return null;
    }

    public String getLoggedInUserUri() {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user == null || !userService.isUserLoggedIn()) {
            return null;
        }

        return "/addama/users/" + user.getEmail();
    }

    public boolean isUserAdmin() {
        UserService userService = UserServiceFactory.getUserService();
        if (userService.getCurrentUser() != null) {
            return userService.isUserAdmin();
        }
        return false;
    }

    public JSONObject whoAmI(Principal userPrincipal, String requestURI) throws JSONException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        JSONObject json = new JSONObject();
        if (user == null || !userService.isUserLoggedIn()) {
            json.put("loginUrl",userService.createLoginURL(requestURI));
            return json;
        }


        json.put("email", user.getEmail());
        if (userPrincipal != null) {
            json.put("name", userPrincipal.getName());
        } else {
            json.put("name", user.getNickname());
        }
        json.put("isAdmin", userService.isUserAdmin());
        json.put("logoutUrl", userService.createLogoutURL(requestURI));
        json.put("uri", "/addama/users/" + user.getEmail());
        json.put("channel", "/addama/channels/" + user.getEmail());
        return json;
    }
}

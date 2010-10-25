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

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.urlfetch.*;
import com.google.apphosting.api.ApiProxy;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.coresvcs.gae.pojos.RegistryService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

/**
 * @author hrovira
 */
public class Sharing {
    private static final String APPSPOT_HOST = ApiProxy.getCurrentEnvironment().getAppId() + ".appspot.com";

    private final MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(getClass().getName());
    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    private Registry registry;
    private Users users;
    private ApiKeys apiKeys;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public void setApiKeys(ApiKeys apiKeys) {
        this.apiKeys = apiKeys;
    }

    /**
     * Checks the sharing service for the registry service to see if the user has access
     *
     * @param registryService - targeted by user
     * @param request         - submitted by user
     * @return boolean - true: sharing service passes; false: no sharing service, or no opinion from sharing service
     * @throws IOException              - problems making request to sharing service
     * @throws ForbiddenAccessException - sharing service forbids access to registry service
     */
    public boolean checkAccess(RegistryService registryService, HttpServletRequest request) throws IOException, ForbiddenAccessException {
        RegistryService sharingService = getSharingService(registryService.getSharingUri());
        if (sharingService != null) {
            String sharingUrl = StringUtils.chomp(sharingService.getUrl().toString(), "/");
            String sharingUri = "/addama/sharing" + StringUtils.chomp(request.getRequestURI(), "/");
            String sharingOp = getSharingOperation(request);
            String accessKey = sharingService.getAccessKey().toString();

            HTTPRequest req = new HTTPRequest(new URL(sharingUrl + sharingUri + sharingOp), HTTPMethod.GET);
            req.setHeader(new HTTPHeader("x-addama-registry-key", accessKey));
            req.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_HOST));
            req.setHeader(new HTTPHeader("x-addama-registry-user", getUserUri(request)));
            String serviceUri = request.getHeader("x-addama-service-uri");
            if (!StringUtils.isEmpty(serviceUri)) {
                req.setHeader(new HTTPHeader("x-addama-service-uri", serviceUri));
            }

            HTTPResponse resp = urlFetchService.fetch(req);
            int responseCode = resp.getResponseCode();
            switch (responseCode) {
                case 200:
                    return true;
                case 400:
                    return false;
                case 403:
                    throw new ForbiddenAccessException();
            }
        }
        return false;
    }

    /*
     * Private Methods
     */

    private RegistryService getSharingService(String sharingUri) {
        if (memcacheService.contains(sharingUri)) {
            return (RegistryService) memcacheService.get(sharingUri);
        }

        RegistryService sharingService = registry.getRegistryService(sharingUri);
        if (sharingService != null) {
            memcacheService.put(sharingUri, sharingService);
        }
        return sharingService;
    }

    private String getSharingOperation(HttpServletRequest request) {
        String method = request.getMethod();
        if (StringUtils.equalsIgnoreCase(method, "get")) {
            return "/read";
        }
        return "/write";
    }

    private String getUserUri(HttpServletRequest request) {
        String userUri = users.getLoggedInUserUri();
        if (!StringUtils.isEmpty(userUri)) {
            return userUri;
        }
        String apikey = request.getHeader("x-addama-apikey");
        if (StringUtils.isEmpty(apikey)) {
            apikey = request.getHeader("API_KEY"); // TODO : deprecated
        }
        return apiKeys.getUserUriFromApiKey(apikey);
    }

}

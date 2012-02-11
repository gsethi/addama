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
package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.urlfetch.*;
import org.systemsbiology.addama.appengine.pojos.RegistryService;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.Appspot.APPSPOT_ID;
import static org.systemsbiology.addama.appengine.util.Registry.getRegistryService;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserEmail;

/**
 * @author hrovira
 */
public class Sharing {
    private static final URLFetchService urlFetchService = getURLFetchService();

    /**
     * Checks the sharing service for the registry service to see if the user has access
     *
     * @param registryService - targeted by user
     * @param request         - submitted by user
     * @return boolean - true: sharing service passes; false: no sharing service, or no opinion from sharing service
     * @throws IOException              - problems making request to sharing service
     * @throws ForbiddenAccessException - sharing service forbids access to registry service
     */
    public static boolean checkAccess(RegistryService registryService, HttpServletRequest request) throws IOException, ForbiddenAccessException {
        RegistryService sharingService = getRegistryService(registryService.getSharingUri());
        if (sharingService != null) {
            String sharingUrl = chomp(sharingService.getUrl().toString(), "/");
            String sharingUri = "/addama/sharing" + chomp(request.getRequestURI(), "/");
            String sharingOp = equalsIgnoreCase(request.getMethod(), "get") ? "/read" : "/write";
            String accessKey = sharingService.getAccessKey().toString();

            HTTPRequest req = new HTTPRequest(new URL(sharingUrl + sharingUri + sharingOp), HTTPMethod.GET);
            req.setHeader(new HTTPHeader("x-addama-registry-key", accessKey));
            req.setHeader(new HTTPHeader("x-addama-registry-host", APPSPOT_ID));
            req.setHeader(new HTTPHeader("x-addama-registry-user", getLoggedInUserEmail(request)));
            String serviceUri = request.getHeader("x-addama-service-uri");
            if (!isEmpty(serviceUri)) {
                req.setHeader(new HTTPHeader("x-addama-service-uri", serviceUri));
            }

            HTTPResponse resp = urlFetchService.fetch(req);
            int responseCode = resp.getResponseCode();
            switch (responseCode) {
                case SC_OK:
                    return true;
                case SC_BAD_REQUEST:
                    return false;
                case SC_FORBIDDEN:
                    throw new ForbiddenAccessException();
            }
        }
        return false;
    }
}

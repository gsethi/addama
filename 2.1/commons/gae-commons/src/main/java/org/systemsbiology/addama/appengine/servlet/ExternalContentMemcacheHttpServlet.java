/*
**    Copyright (C) 2003-2011 Institute for Systems Biology
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
package org.systemsbiology.addama.appengine.servlet;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.users.UserService;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;
import org.systemsbiology.addama.coresvcs.gae.pojos.HTTPResponseContent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate.loadIfNotExisting;

/**
 * @author hrovira
 */

/**
 * <p/>Servlet that helps to manage HTML content served from outside appspot.
 * <p/>
 * <ul>Features:
 * <li>Allows content from any number of external servers</li>
 * <li>Stores content for future requests in memcache</li>
 * <li>Enforces user login on HTML content</li>
 * <li>Configuration driven through System properties (appengine-web.xml)</li>
 * </ul>
 * <p/>
 * <p/>Example Configuration:
 * <property name="org.systemsbiology.addama.appengine.servlet.externalcontent.a"
 * value="http://addama.googlecode.com/svn/.../html"/>
 * <property name="org.systemsbiology.addama.appengine.servlet.externalcontent.b"
 * value="https://contentroot.example.org"/>
 */
public class ExternalContentMemcacheHttpServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ExternalContentMemcacheHttpServlet.class.getName());

    public static final String CONFIG_EXTERNAL_KEY = "org.systemsbiology.addama.appengine.servlet.externalcontent.";

    private final MemcacheService externalContent = getMemcacheService("external-content");
    private final UserService userService = getUserService();
    private final HashSet<String> baseUrls = new HashSet<String>();

    /**
     * Configures this servlet from System Properties starting with 'org.systemsbiology.addama.appengine.servlet.externalcontent.'
     * Expects URL for external servers.
     *
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        super.init();

        try {
            for (Map.Entry entry : System.getProperties().entrySet()) {
                String pname = (String) entry.getKey();
                if (pname.startsWith(CONFIG_EXTERNAL_KEY)) {
                    baseUrls.add((String) entry.getValue());
                }
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }

    /**
     * Retrieves external content from a variety of mapped content providers, caches the response for future requests
     *
     * @param request  - HttpServletRequest
     * @param response - HttpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        // lookup external content
        HTTPResponseContent content = getExternalContent(requestUri);
        if (content == null) {
            response.setStatus(SC_NOT_FOUND);
            return;
        }

        // enforce login if required on HTML pages... not JS, CSS or Images
        if (content.isHtml() && !userService.isUserLoggedIn()) {
            response.sendRedirect(userService.createLoginURL(requestUri));
            return;
        }

        String mimeType = getServletContext().getMimeType(requestUri);
        if (!isEmpty(mimeType)) {
            response.setContentType(mimeType);
        } else {
            // output external content
            String contentType = content.getContentType();
            if (!isEmpty(contentType)) {
                response.setContentType(content.getContentType());
            }
        }
        response.getOutputStream().write(content.getBytes());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (userService.isUserLoggedIn() && userService.isUserAdmin()) {
            log.info("clearing external content cache");
            externalContent.clearAll();

            response.setStatus(SC_OK);
            return;
        }

        response.setStatus(SC_FORBIDDEN);
    }

    /**
     * Retrieves the requested URI from the first mapped external URL it can find
     *
     * @param requestUri - targeted content uri
     * @return HTTPResponseContent - POJO containing response and HTML content-type detecting logic
     */
    private HTTPResponseContent getExternalContent(String requestUri) {
        for (final String baseUrl : baseUrls) {
            try {
                MemcacheLoaderCallback callback = new MemcacheLoaderCallback() {
                    public Serializable getCacheableObject(String uri) throws Exception {
                        String externalUrl = baseUrl + uri;

                        HTTPResponse resp = getURLFetchService().fetch(new URL(externalUrl));
                        if (resp.getResponseCode() == SC_OK) {
                            log.info("loaded:" + externalUrl);
                            return new HTTPResponseContent(resp);
                        }
                        return null;
                    }
                };

                HTTPResponseContent rc = (HTTPResponseContent) loadIfNotExisting(externalContent, requestUri, callback);
                if (rc != null) {
                    return rc;
                }
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }
        return null;
    }
}
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate.loadIfNotExisting;
import static org.systemsbiology.addama.coresvcs.gae.pojos.HTTPResponseContent.serveContent;

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
 * <property name="org.systemsbiology.addama.externalcontent.AAA" value="http://example.com/contentroot"/>
 * <property name="org.systemsbiology.addama.externalcontent.BBB" value="http://example.com/othercontent/html"/>
 * <p/>
 * <ul>Resolves Content:
 * <li>https://example.appspot.com/servlet-context/AAA/content.html to http://example.com/contentroot/content.html</li>
 * <li>https://example.appspot.com/servlet-context/BBB/content.html to http://example.com/othercontent/html/content.html</li>
 * </ul>
 */
public class ExternalContentMemcacheHttpServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ExternalContentMemcacheHttpServlet.class.getName());

    public static final String CONFIG_EXTERNAL_KEY = "org.systemsbiology.addama.externalcontent";

    private final MemcacheService externalContent = getMemcacheService("external-content");
    private final UserService userService = getUserService();
    private final HashMap<String, URL> contentEntries = new HashMap<String, URL>();

    /**
     * Configures this servlet from System Properties starting with 'org.systemsbiology.addama.externalcontent.'
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
                    String baseContentUri = substringAfterLast(pname, CONFIG_EXTERNAL_KEY + ".");
                    contentEntries.put(baseContentUri, new URL((String) entry.getValue()));
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
        HTTPResponseContent content = lookupExternalContent(request);
        serveContent(content, request, response);
    }

    /**
     * Allows administrators to reset the memcache for external content
     *
     * @param request  - http
     * @param response - http
     * @throws ServletException
     * @throws IOException
     */
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

    /*
     * Private Methods
     */
    private HTTPResponseContent lookupExternalContent(HttpServletRequest request) {
        try {
            String externalContentKey = substringBetween(request.getRequestURI(), request.getContextPath(), "/");
            if (this.contentEntries.containsKey(externalContentKey)) {
                URL contentUrl = this.contentEntries.get(externalContentKey);
                MemcacheLoaderCallback callback = new ExternalContentMemcacheLoaderCallback(contentUrl);

                String targetUri = substringAfterLast(request.getRequestURI(), externalContentKey + "/");
                return (HTTPResponseContent) loadIfNotExisting(externalContent, targetUri, callback);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return null;
    }

    private class ExternalContentMemcacheLoaderCallback implements MemcacheLoaderCallback {
        private final URL contentUrl;

        private ExternalContentMemcacheLoaderCallback(URL contentUrl) {
            this.contentUrl = contentUrl;
        }

        public Serializable getCacheableObject(String uri) throws Exception {
            URL targetUrl = getTargetUrl(uri);
            log.info("loading:" + targetUrl);

            HTTPResponse resp = getURLFetchService().fetch(targetUrl);
            if (resp.getResponseCode() == SC_OK) {
                log.info("loaded:" + targetUrl);
                return new HTTPResponseContent(resp);
            }
            return null;
        }

        private URL getTargetUrl(String desired) throws MalformedURLException {
            String url = chomp(contentUrl.toString(), "/");
            String uri = desired;
            if (uri.startsWith("/")) {
                uri = substringAfter(desired, "/");
            }
            return new URL(url + "/" + uri);
        }
    }
}
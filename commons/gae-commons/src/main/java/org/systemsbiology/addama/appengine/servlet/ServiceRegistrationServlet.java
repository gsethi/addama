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
package org.systemsbiology.addama.appengine.servlet;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.systemsbiology.addama.appengine.datastore.PutEntityTransactionCallback;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static com.google.appengine.api.urlfetch.HTTPMethod.POST;
import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.Appspot.APP_ID;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class ServiceRegistrationServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ServiceRegistrationServlet.class.getName());

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String regPage = getRegistrationPage();
        log.fine("sending to registration page:" + regPage);
        response.sendRedirect(regPage);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.fine(request.getRequestURI());

        try {
            RegistrationBean rb = getRegistrationBean(request);
            if (!rb.isValid()) {
                throw new Exception("Missing required fields [host,apikey,owner]");
            }

            ServiceConfig config = new ServiceConfig();
            config.setServletContext(super.getServletContext());
            doRegistration(config, rb);

            broadcastSuccess(rb);
            response.sendRedirect(getRegistrationPage() + "?success=true");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.sendRedirect(getRegistrationPage() + "?success=false");
        }
    }

    /*
     * Private Methods
     */
    private void doRegistration(ServiceConfig serviceConfig, RegistrationBean rb) throws Exception {
        URL registryUrl = new URL(rb.getHttpsHost() + "/addama/registry");

        JSONObject registration = new JSONObject();
        registration.put("url", "https://" + getCurrentEnvironment().getAttributes().get("com.google.appengine.runtime.default_version_hostname"));
        registration.put("label", serviceConfig.LABEL());
        registration.put("searchable", serviceConfig.JSON().optBoolean("searchable", false));

        for (Mapping m : serviceConfig.getMappings()) {
            JSONObject mapping = new JSONObject();
            mapping.put("uri", m.URI());
            mapping.put("label", m.LABEL());
            if (m.JSON().has("family")) {
                mapping.put("family", chomp(m.JSON().getString("family"), "/"));
            }
            if (m.JSON().has("pattern")) {
                mapping.put("pattern", m.JSON().getString("pattern"));
            }
            registration.append("mappings", mapping);
        }

        HTTPRequest post = new HTTPRequest(registryUrl, POST);
        post.setHeader(new HTTPHeader("x-addama-apikey", rb.apikey));
        post.setPayload(("registration=" + registration.toString()).getBytes());

        HTTPResponse resp = getURLFetchService().fetch(post);
        if (resp.getResponseCode() == SC_OK) {
            List<HTTPHeader> headers = resp.getHeaders();
            if (headers != null) {
                for (HTTPHeader header : headers) {
                    if (equalsIgnoreCase(header.getName(), "x-addama-registry-key")) {
                        String registryKey = header.getValue();
                        if (!isEmpty(registryKey)) {
                            Entity e = new Entity(createKey("registration", registryUrl.getHost()));
                            e.setProperty("REGISTRY_SERVICE_KEY", registryKey);
                            inTransaction(getDatastoreService(), new PutEntityTransactionCallback(e));
                        }
                    }
                }
            }
        }
    }

    private String getRegistrationPage() {
        String regPage = System.getProperty("gae.addama.registration.page");
        if (!isEmpty(regPage)) {
            return regPage;
        }
        return "/registration.html";
    }

    private RegistrationBean getRegistrationBean(HttpServletRequest request) throws Exception {
        String loggedInAdmin = getEmail();

        if (isMultipartContent(request)) {
            RegistrationBean rb = extractRegistrationFromFile(request);
            if (!rb.isValid()) {
                throw new Exception("Missing required fields [host,apikey,owner]");
            }

            if (equalsIgnoreCase(rb.owner, loggedInAdmin)) {
                log.warning("Registration Keys Don't Match Owner [" + rb.owner + "," + loggedInAdmin + "]");
                throw new Exception("Invalid API Keys.  Please try generating a new set.");
            }

            return rb;
        }

        String host = request.getParameter("host");
        String apikey = request.getParameter("apikey");
        return new RegistrationBean(loggedInAdmin, host, apikey);
    }

    private RegistrationBean extractRegistrationFromFile(HttpServletRequest request) throws Exception {
        ServletFileUpload fileUpload = new ServletFileUpload();
        FileItemIterator itemIterator = fileUpload.getItemIterator(request);

        if (itemIterator.hasNext()) {
            FileItemStream itemStream = itemIterator.next();
            if (!itemStream.isFormField()) {
                Properties p = new Properties();
                p.load(itemStream.openStream());

                String owner = p.getProperty("owner");
                String host = p.getProperty("host");
                String apikey = p.getProperty("apikey");
                return new RegistrationBean(owner, host, apikey);
            }
        }

        return null;
    }

    private void broadcastSuccess(RegistrationBean rb) {
        try {
            String url = rb.getHttpsHost() + "/addama/channels/" + rb.owner;
            JSONObject json = new JSONObject();
            json.put("message", "Successfully Registered [" + APP_ID + "] to [" + rb.host + "]");

            getDefaultQueue().add(withUrl(url).param("event", json.toString()).header("x-addama-apikey", rb.apikey));
        } catch (Exception e) {
            log.warning("unable to broadcast success news to " + rb.owner + "," + rb.host);
        }
    }

    private String getEmail() throws Exception {
        UserService userService = getUserService();
        if (userService.isUserLoggedIn() && userService.isUserAdmin()) {
            User user = userService.getCurrentUser();
            return user.getEmail();
        }
        throw new Exception("logged in user must be administrator of this appspot to be able to register");
    }

    private class RegistrationBean {
        private final String owner;
        private final String host;
        private final String apikey;

        public RegistrationBean(String owner, String host, String apikey) {
            this.owner = owner;
            this.host = host;
            this.apikey = apikey;
        }

        public boolean isValid() {
            return !isEmpty(owner) && !isEmpty(host) && !isEmpty(apikey);
        }

        public String getHttpsHost() {
            if (host.startsWith("https://")) {
                return host;
            }

            return "https://" + host;
        }
    }

}


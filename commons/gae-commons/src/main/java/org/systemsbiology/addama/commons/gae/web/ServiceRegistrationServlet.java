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
package org.systemsbiology.addama.commons.gae.web;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.config.ServiceRegistrationMappingsHandler;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import static com.google.appengine.api.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.commons.gae.Appspot.APP_ID;

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
            config.visit(new ServiceRegistrationMappingsHandler(config, rb.getHttpsHost(), rb.apikey));

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


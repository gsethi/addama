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

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.systemsbiology.addama.commons.gae.config.ServiceRegistrationJsonConfigHandler;
import org.systemsbiology.addama.jsonconfig.JsonConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.systemsbiology.addama.commons.gae.Appspot.APP_ID;

/**
 * @author hrovira
 */
public class ServiceRegistrationServlet extends HttpServlet {
    private static final Queue queue = QueueFactory.getDefaultQueue();

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
            doRegistration(rb);
            broadcastSuccess(rb);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }

        response.sendRedirect(getRegistrationPage() + "?success=true");
    }

    /*
     * Private Methods
     */

    private String getRegistrationPage() {
        String regPage = System.getProperty("gae.addama.registration.page");
        if (!StringUtils.isEmpty(regPage)) {
            return regPage;
        }
        return "/registration.html";
    }

    private RegistrationBean getRegistrationBean(HttpServletRequest request) throws Exception {
        if (ServletFileUpload.isMultipartContent(request)) {
            return getRegistrationByFile(request);
        }
        return getRegistrationByParameters(request);
    }

    private RegistrationBean getRegistrationByFile(HttpServletRequest request) throws Exception {
        RegistrationBean rb = extractRegistrationFromFile(request);
        if (!rb.isValid()) {
            throw new Exception("Missing required fields [host,apikey,owner]");
        }

        String ownerFromRequest = StringUtils.substringAfterLast(request.getHeader("x-addama-registry-user"), "/addama/users/");
        if (StringUtils.equalsIgnoreCase(rb.getOwner(), ownerFromRequest)) {
            log.warning("Registration Keys Don't Match Owner [" + rb.getOwner() + "," + ownerFromRequest + "]");
            throw new Exception("Invalid API Keys.  Please try generating a new set.");
        }

        return rb;
    }

    private RegistrationBean getRegistrationByParameters(HttpServletRequest request) throws Exception {
        RegistrationBean rb = new RegistrationBean();
        rb.setHost(request.getParameter("host"));
        rb.setApikey(request.getParameter("apikey"));
        rb.setOwner(request.getHeader("x-addama-registry-user"));
        return rb;
    }

    private RegistrationBean extractRegistrationFromFile(HttpServletRequest request) throws FileUploadException, IOException {
        ServletFileUpload fileUpload = new ServletFileUpload();
        FileItemIterator itemIterator = fileUpload.getItemIterator(request);

        RegistrationBean rb = new RegistrationBean();
        if (itemIterator.hasNext()) {
            FileItemStream itemStream = itemIterator.next();
            if (!itemStream.isFormField()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(itemStream.openStream()));
                String line = "";
                while (line != null) {
                    line = reader.readLine();
                    rb.readLine(line);
                }
            }
        }
        return rb;
    }

    private void doRegistration(RegistrationBean rb) throws Exception {
        if (!rb.isValid()) {
            throw new Exception("Missing required fields [host,apikey,owner]");
        }

        JsonConfig config = new JsonConfig(new ClassPathResource(System.getProperty("gae.addama.registration.service.jsonConfig")));
        config.visit(new ServiceRegistrationJsonConfigHandler(rb.getHttpsHost(), rb.getApikey()));
    }

    private void broadcastSuccess(RegistrationBean rb) {
        try {
            String url = rb.getHttpsHost() + "/addama/channels/" + rb.getOwner();
            JSONObject json = new JSONObject();
            json.put("message", "Successfully Registered [" + APP_ID + "] to [" + rb.getHost() + "]");

            queue.add(withUrl(url).param("event", json.toString()).header("x-addama-apikey", rb.getApikey()));
        } catch (Exception e) {
            log.warning("unable to broadcast success news to " + rb.getOwner() + "," + rb.getHost());
        }
    }

    private class RegistrationBean {
        private String owner;
        private String host;
        private String apikey;

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getApikey() {
            return apikey;
        }

        public void setApikey(String apikey) {
            this.apikey = apikey;
        }

        public boolean isValid() {
            return !StringUtils.isEmpty(owner) && !StringUtils.isEmpty(host) && !StringUtils.isEmpty(apikey);
        }

        public String getHttpsHost() {
            if (host.startsWith("https://")) {
                return host;
            }

            return "https://" + host;
        }

        public void readLine(String line) {
            if (StringUtils.isEmpty(line)) {
                return;
            }

            String h = getValueFromLine(line, "host");
            if (!StringUtils.isEmpty(h)) {
                this.host = h;
            }

            String a = getValueFromLine(line, "apikey");
            if (!StringUtils.isEmpty(a)) {
                this.apikey = a;
            }

            String o = getValueFromLine(line, "owner");
            if (!StringUtils.isEmpty(o)) {
                this.owner = o;
            }
        }

        private String getValueFromLine(String line, String key) {
            if (StringUtils.contains(line, key + "=")) {
                return StringUtils.substringAfterLast(line, key + "=");
            }
            return null;
        }

    }


}


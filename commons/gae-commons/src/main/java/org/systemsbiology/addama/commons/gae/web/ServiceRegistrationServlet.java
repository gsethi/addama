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

import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.gae.config.ServiceRegistrationJsonConfigHandler;
import org.systemsbiology.addama.registry.JsonConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ServiceRegistrationServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ServiceRegistrationServlet.class.getName());

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.fine("registrationPage");
        response.sendRedirect(System.getProperty("gae.addama.registration.page"));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.fine("registration(" + request.getRequestURI() + ")");

        try {
            String registryDomain = request.getParameter("registry_domain");
            String apiKey = request.getParameter("api_key");
            if (StringUtils.isEmpty(registryDomain) || StringUtils.isEmpty(apiKey)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setConfig(System.getProperty("gae.addama.registration.service.jsonConfig"));
            jsonConfig.processConfiguration(new ServiceRegistrationJsonConfigHandler(registryDomain, apiKey));
        } catch (Exception e) {
            log.warning("registration(" + request.getRequestURI() + "): registration failed: " + e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}
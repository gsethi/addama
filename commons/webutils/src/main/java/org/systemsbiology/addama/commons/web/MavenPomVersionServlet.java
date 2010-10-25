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
package org.systemsbiology.addama.commons.web;

import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class MavenPomVersionServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(MavenPomVersionServlet.class.getName());

    private ServletContext servletContext;
    private String versionInfo;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.servletContext = servletConfig.getServletContext();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().print(getVersion());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    private String getVersion() throws ServletException {
        if (StringUtils.isEmpty(this.versionInfo)) {
            try {
                String pomLocation = getInitParameter("pom_location");
                if (StringUtils.isEmpty(pomLocation)) {
                    this.versionInfo = "POM Location Not Specified [" + pomLocation + "]";
                    log.warning(this.versionInfo);

                } else {
                    InputStream inputStream = this.servletContext.getResourceAsStream(pomLocation + "/pom.properties");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder builder = new StringBuilder();
                    String line = "";
                    while (line != null) {
                        line = reader.readLine();
                        if (line != null) {
                            builder.append(line).append("\n");
                        }
                    }
                    this.versionInfo = builder.toString();
                }
            } catch (Exception e) {
                log.warning("init:" + e);
                this.versionInfo = e.getMessage();
            }
        }
        return this.versionInfo;
    }
}

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
package org.systemsbiology.addama.appengine.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.appengine.pojos.ApiKey;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.appengine.util.ApiKeys.getUserApiKey;
import static org.systemsbiology.addama.appengine.util.Users.checkAdmin;
import static org.systemsbiology.addama.appengine.util.Users.getCurrentUser;
import static org.systemsbiology.addama.appengine.Appspot.*;

/**
 * @author hrovira
 */
@Controller
public class ApiKeyController {
    private static final Logger log = Logger.getLogger(ApiKeyController.class.getName());

    @RequestMapping(value = "/apikeys", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView apikeys(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        ApiKey apiKey = getUserApiKey();
        return new ModelAndView(new JsonView()).addObject("json", apiKey.toJSON());
    }

    @RequestMapping(value = "/apikeys/addama.properties", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView addama_properties(HttpServletRequest request, HttpServletResponse response,
                                          @RequestParam(value = "serviceUrl", required = false) String serviceUrl) throws Exception {
        log.info(request.getRequestURI());

        checkAdmin(request);

        ApiKey apiKey = getUserApiKey();

        StringBuilder builder = new StringBuilder();
        builder.append("httpclient.secureHostUrl=").append(APPSPOT_URL).append("\n");
        builder.append("httpclient.apikey=").append(apiKey.getKey().toString()).append("\n");
        if (!isEmpty(serviceUrl)) {
            URL hostUrl = new URL(serviceUrl);
            builder.append("service.hostUrl=").append(hostUrl.toString()).append("\n");
        }

        outputFile(response, "addama.properties", builder.toString());

        return new ModelAndView(new OkResponseView());
    }

    @RequestMapping(value = "/apikeys/file", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView apikey_file(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info(request.getRequestURI());

        ApiKey apiKey = getUserApiKey();

        StringBuilder builder = new StringBuilder();
        builder.append("[Connection]");
        builder.append("\n").append("host=").append(APPSPOT_ID);
        builder.append("\n").append("apikey=").append(apiKey.getKey().toString());
        builder.append("\n").append("owner=").append(getCurrentUser().getEmail());
        builder.append("\n");

        outputFile(response, APP_ID + ".apikey", builder.toString());

        return new ModelAndView(new OkResponseView());
    }

    /*
     * Private Methods
     */

    private void outputFile(HttpServletResponse response, String filename, String content) throws IOException {
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(content.getBytes());
    }
}
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
package org.systemsbiology.addama.rest.views;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.View;
import org.systemsbiology.addama.rest.util.UriBuilder;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author hrovira
 */
public class PostJsonView implements View {

    public String getContentType() {
        return "application/json";
    }

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (ServletRequestUtils.getBooleanParameter(request, "NO_RESPONSE", false)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String alternateView = ServletRequestUtils.getStringParameter(request, "REDIRECT_TO");
        if (!StringUtils.isEmpty(alternateView)) {
            Node node = (Node) map.get("node");

            UriBuilder uriBuilder = new UriBuilder(request);
            String uri = uriBuilder.getUri(node);
            if (StringUtils.contains(alternateView, "?")) {
                response.sendRedirect(alternateView + "&uri=" + uri);
            } else {
                response.sendRedirect(alternateView + "?uri=" + uri);
            }
            return;
        }

        response.setContentType(getContentType());

        JSONObject json = (JSONObject) map.get("json");

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }
}

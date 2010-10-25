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
package org.systemsbiology.addama.commons.web.views;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author hrovira
 */
public class JsonResultsView extends JsonView {

    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject json = (JSONObject) map.get("json");
        if (json == null) json = new JSONObject();

        if (!json.has("results")) {
            json.put("results", new JSONArray());
        }

        json.put("numberOfResults", json.getJSONArray("results").length());

        response.setContentType(getContentType(request));

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }

}
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
package org.systemsbiology.addama.aclsvc.service;

import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.web.servlet.View;

/**
 * @author trobinso (adapted from the MRMAtlas project)
 */

public class JsonThrowableView implements View {

    private final Throwable t;

    public JsonThrowableView(Throwable t) {
        this.t = t;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(Map map,
                       HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        json.put("success", false);
        JSONObject errorJson = new JSONObject();
        String msg = "";
        if (t.getClass().getCanonicalName() != null) {
            msg += t.getClass().getCanonicalName() + ": ";
        }
        msg += t.getMessage();
        errorJson.put("msg", msg);
        json.put("error", errorJson);

        response.setContentType(getContentType());
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }
}

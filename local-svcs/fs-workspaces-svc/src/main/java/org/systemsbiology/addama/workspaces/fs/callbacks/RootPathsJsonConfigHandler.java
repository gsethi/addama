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
package org.systemsbiology.addama.workspaces.fs.callbacks;

import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import java.util.Map;

/**
 * @author hrovira
 */
public class RootPathsJsonConfigHandler implements JsonConfigHandler {
    private final Map<String, String> rootPathsByUri;

    public RootPathsJsonConfigHandler(Map<String, String> rootPathsByUri) {
        this.rootPathsByUri = rootPathsByUri;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("locals")) {
            JSONArray locals = configuration.getJSONArray("locals");
            for (int i = 0; i < locals.length(); i++) {
                JSONObject local = locals.getJSONObject(i);
                rootPathsByUri.put(local.getString("uri"), local.getString("rootPath"));
            }
        }
    }
}
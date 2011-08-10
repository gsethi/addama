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
package org.systemsbiology.addama.jsonconfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import static org.systemsbiology.addama.jsonconfig.JsonConfigHandler.Keys.configuration;
import static org.systemsbiology.addama.jsonconfig.JsonConfigHandler.Keys.configurations;


/**
 * @author hrovira
 */
public class JsonConfig {
    private static final Logger log = Logger.getLogger(JsonConfig.class.getName());

    private final JSONObject JSON;

    public JsonConfig(Resource resource) throws Exception {
        InputStream inputStream = resource.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder builder = new StringBuilder();
        String line = "";
        while (line != null) {
            line = bufferedReader.readLine();
            if (line != null) {
                builder.append(line);
            }
        }

        this.JSON = new JSONObject(builder.toString());
    }

    public JsonConfig(JSONObject configuration) {
        this.JSON = configuration;
    }

    public void visit(JsonConfigHandler handler) {
        try {
            if (JSON.has(configuration.name())) {
                handler.handle(JSON.getJSONObject(configuration.name()));
            } else if (JSON.has(configurations.name())) {
                JSONArray services = JSON.getJSONArray(configurations.name());
                for (int i = 0; i < services.length(); i++) {
                    handler.handle(services.getJSONObject(i));
                }
            } else {
                handler.handle(JSON);
            }
        } catch (Exception e) {
            try {
                log.warning("errors processing configuration:\n" + JSON.toString(4));
            } catch (JSONException ex) {
                log.warning("errors printing configuration:" + ex.getMessage());
            }
            throw new RuntimeException(e);
        }
    }
}
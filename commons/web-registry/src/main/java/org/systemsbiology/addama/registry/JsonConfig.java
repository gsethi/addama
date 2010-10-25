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
package org.systemsbiology.addama.registry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class JsonConfig {
    private static final Logger log = Logger.getLogger(JsonConfig.class.getName());

    private String config;

    public void setConfig(String value) {
        this.config = value;
    }

    public JSONObject getConfiguration() throws Exception {
        ClassPathResource resource = new ClassPathResource(this.config);

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

        return new JSONObject(builder.toString());
    }

    public void processConfiguration(JsonConfigHandler handler) throws Exception {
        try {
            JSONObject config = this.getConfiguration();
            if (config.has("configuration")) {
                handler.handle(config.getJSONObject("configuration"));
            } else if (config.has("configurations")) {
                JSONArray services = config.getJSONArray("configurations");
                for (int i = 0; i < services.length(); i++) {
                    handler.handle(services.getJSONObject(i));
                }
            } else {
                handler.handle(config);
            }
        } catch (Exception e) {
            log.warning("errors processing configuration for " + config + ": " + e.getMessage());
        }
    }

}
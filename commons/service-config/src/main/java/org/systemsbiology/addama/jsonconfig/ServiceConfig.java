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
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.chomp;


/**
 * @author hrovira
 */
public class ServiceConfig implements ServletContextAware {
    private static final Logger log = Logger.getLogger(ServiceConfig.class.getName());

    private final Map<String, Mapping> mappingsById = new HashMap<String, Mapping>();
    private JSONObject JSON;
    private String ID;
    private String LABEL;

    public void setServletContext(ServletContext servletContext) {
        try {
            ClassPathResource resource = new ClassPathResource(servletContext.getContextPath() + ".config");
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
            this.ID = this.JSON.getString("id");
            this.LABEL = this.JSON.getString("label");

            if (JSON.has("mappings")) {
                String family = chomp(JSON.getString("family"), "/");
                JSONArray mappings = JSON.getJSONArray("mappings");
                for (int i = 0; i < mappings.length(); i++) {
                    Mapping m = new Mapping(family, mappings.getJSONObject(i));
                    mappingsById.put(m.ID(), m);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Mapping[] getMappings() {
        Collection<Mapping> mappings = mappingsById.values();
        return mappings.toArray(new Mapping[mappings.size()]);
    }

    public Mapping getMapping(String id) {
        return mappingsById.get(id);
    }

    public String ID() {
        return this.ID;
    }

    public String LABEL() {
        return this.LABEL;
    }

    public JSONObject JSON() {
        return this.JSON;
    }

    public void visit(MappingsHandler handler) throws Exception {
        try {
            for (Mapping mapping : mappingsById.values()) {
                handler.handle(mapping);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
            log.info(this.JSON.toString());
            throw e;
        }
    }

}
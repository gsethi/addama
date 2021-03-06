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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.chomp;
import static org.apache.commons.lang.StringUtils.substringAfter;


/**
 * @author hrovira
 */
public class ServiceConfig implements ServletContextAware {
    private static final Logger log = Logger.getLogger(ServiceConfig.class.getName());

    private final Map<String, Mapping> mappingsById = new HashMap<String, Mapping>();
    private JSONObject JSON;
    private String LABEL;
    private String FAMILY;

    public void setServletContext(ServletContext servletContext) {
        try {
            BufferedReader reader = readConfig(servletContext);
            if (reader != null) {
                StringBuilder builder = new StringBuilder();
                String line = "";
                while (line != null) {
                    line = reader.readLine();
                    if (line != null) {
                        builder.append(line);
                    }
                }

                this.JSON = new JSONObject(builder.toString());
                this.LABEL = this.JSON.getString("label");
                this.FAMILY = this.JSON.getString("family");

                if (JSON.has("mappings")) {
                    JSONArray mappings = JSON.getJSONArray("mappings");
                    log.info("adding mappings [" + this.FAMILY + "," + mappings.length() + "]");
                    for (int i = 0; i < mappings.length(); i++) {
                        Mapping m = new Mapping(mappings.getJSONObject(i));
                        mappingsById.put(m.ID(), m);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Mapping> getMappings() {
        return mappingsById.values();
    }

    public Mapping getMapping(String id) {
        return mappingsById.get(id);
    }

    public String LABEL() {
        return this.LABEL;
    }

    public String FAMILY() {
        return this.FAMILY;
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

    private BufferedReader readConfig(ServletContext servletContext) {
        try {
            String contextPath = servletContext.getContextPath();
            if (contextPath.startsWith("/")) {
                contextPath = substringAfter(contextPath, "/");
            }

            String configPath = "services/" + contextPath + ".config";
            log.info("loading:" + configPath);

            ClassPathResource resource = new ClassPathResource(configPath);
            InputStream inputStream = resource.getInputStream();
            return new BufferedReader(new InputStreamReader(inputStream));
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return null;
    }
}
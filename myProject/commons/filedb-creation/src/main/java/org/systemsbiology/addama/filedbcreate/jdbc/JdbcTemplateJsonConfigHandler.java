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
package org.systemsbiology.addama.filedbcreate.jdbc;

import org.apache.commons.dbcp.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import java.util.Map;

/**
 * @author hrovira
 */
public class JdbcTemplateJsonConfigHandler implements JsonConfigHandler {
    private final Map<String, JdbcTemplateDataSource> datasourcesByUri;
    private final Map<String, String> rootPathByUri;

    public JdbcTemplateJsonConfigHandler(Map<String, JdbcTemplateDataSource> datasourcesByUri, Map<String, String> rootPathByUri) {
        this.datasourcesByUri = datasourcesByUri;
        this.rootPathByUri = rootPathByUri;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("locals")) {
            JSONArray locals = configuration.getJSONArray("locals");
            for (int i = 0; i < locals.length(); i++) {
                JSONObject local = locals.getJSONObject(i);

                BasicDataSource bds = new BasicDataSource();
                bds.setDefaultAutoCommit(false);
                bds.setDriverClassName(local.getString("classname"));
                bds.setUrl(local.getString("jdbcurl"));
                if (local.has("username")) {
                    bds.setUsername(local.getString("username"));
                }
                if (local.has("password")) {
                    bds.setPassword(local.getString("password"));
                }

                String uri = local.getString("uri");

                JdbcTemplate jdbcTemplate = new JdbcTemplate(bds);
                if (local.has("maxrows")) {
                    jdbcTemplate.setMaxRows(local.getInt("maxrows"));
                }
                jdbcTemplate.afterPropertiesSet();

                JdbcTemplateDataSource newDataSource = new JdbcTemplateDataSource();
                newDataSource.setJdbcTemplate(jdbcTemplate);
                newDataSource.setDatabaseUri(uri);

                datasourcesByUri.put(uri, newDataSource);
                rootPathByUri.put(uri, local.getString("rootPath"));
            }
        }

    }
}

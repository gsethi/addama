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
package org.systemsbiology.google.visualization.datasource.mvc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateDataTableGenerator;
import org.systemsbiology.google.visualization.datasource.jdbc.JdbcTemplateJsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.getCleanUri;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow;

/**
 * @author hrovira
 */
@Controller
public class QueryController {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());

    protected final Map<String, JdbcTemplate> jdbcTemplatesByUri = new HashMap<String, JdbcTemplate>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new JdbcTemplateJsonConfigHandler(jdbcTemplatesByUri));
    }

    @RequestMapping(method = RequestMethod.GET)
    public void query(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String databaseUri = getCleanUri(request, "/query");

        log.info(databaseUri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(databaseUri);
        String tableName = "T_" + Math.abs(databaseUri.hashCode());
        log.info(databaseUri + ":" + tableName);

        executeDataSourceServletFlow(request, response, new JdbcTemplateDataTableGenerator(jdbcTemplate, tableName));
    }

    protected JdbcTemplate getJdbcTemplate(String databaseUri) throws ResourceNotFoundException {
        for (Map.Entry<String, JdbcTemplate> entry : jdbcTemplatesByUri.entrySet()) {
            if (databaseUri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new ResourceNotFoundException(databaseUri);
    }

}
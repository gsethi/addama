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
package org.systemsbiology.addama.datasources.rest;

import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow;

/**
 * @author hrovira
 */
@Controller
public class DatasourceServiceController {
    private static final Logger log = Logger.getLogger(DatasourceServiceController.class.getName());

    private final Map<String, JdbcTemplate> jdbcTemplateDsByDatabaseUri = new HashMap<String, JdbcTemplate>();
    private final Map<String, String> uriMappingsByDatasource = new HashMap<String, String>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new StringMapJsonConfigHandler(uriMappingsByDatasource, "uriMappings"));
        jsonConfig.visit(new JdbcTemplateJsonConfigHandler(jdbcTemplateDsByDatabaseUri));
    }

    /*
     * Controllers
     */

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.info("processRequest(" + requestUri + ")");

        if (equalsIgnoreCase(requestUri, request.getContextPath())) {
            return getDatabases();
        }

        String databaseUri = getDatabaseUri(request);
        if (isEmpty(databaseUri)) {
            return getDatabases();
        }

        JdbcTemplate jdbcTemplate = jdbcTemplateDsByDatabaseUri.get(databaseUri);
        if (jdbcTemplate == null) {
            throw new ResourceNotFoundException(databaseUri);
        }

        if (requestUri.endsWith("/query") || requestUri.endsWith("/google-ds-api")) {
            String tableUri = getTargetUri(request);
            String tableName = getTableName(tableUri, databaseUri, jdbcTemplate);
            log.info("processRequest(" + requestUri + "):" + tableName);

            SimpleSqlDataTableGenerator tableGenerator = new SimpleSqlDataTableGenerator(jdbcTemplate, tableName);
            executeDataSourceServletFlow(request, response, tableGenerator);
            return null;
        }

        String targetUri = getTargetUri(request);
        if (equalsIgnoreCase(targetUri, databaseUri)) {
            return getDatabase(databaseUri, jdbcTemplate);
        }

        if (equalsIgnoreCase(requestUri, databaseUri)) {
            return getDatabase(databaseUri, jdbcTemplate);
        }

        return getTable(targetUri, databaseUri, jdbcTemplate);
    }

    /*
     * Protected Methods
     */

    protected ModelAndView getDatabases() throws Exception {
        log.info("getDatabases()");

        JSONObject json = new JSONObject();
        for (String databaseUri : jdbcTemplateDsByDatabaseUri.keySet()) {
            json.append("items", new JSONObject().put("uri", databaseUri));
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

    protected ModelAndView getDatabase(String databaseUri, JdbcTemplate jdbcTemplate) throws Exception {
        JSONObject json = new JSONObject();
        for (String tableUri : getTableUris(databaseUri, jdbcTemplate)) {
            JSONObject tableJson = new JSONObject();
            tableJson.put("name", substringAfterLast(tableUri, "/"));
            tableJson.put("uri", tableUri);
            json.append("items", tableJson);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    protected ModelAndView getTable(String tableUri, String databaseUri, JdbcTemplate jdbcTemplate) throws Exception {
        log.info("getTable(" + tableUri + ")");

        String tableName = getTableName(tableUri, databaseUri, jdbcTemplate);
        JSONObject json = (JSONObject) jdbcTemplate.execute(new DatabaseTableColumnConnectionCallback(tableName));

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

    /*
     * Private Methods
     */

    private String getDatabaseUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String targetUri = getTargetUri(request);
        for (String databaseUri : jdbcTemplateDsByDatabaseUri.keySet()) {
            if (targetUri.startsWith(databaseUri)) {
                return databaseUri;
            }
            if (requestUri.startsWith(databaseUri)) {
                return databaseUri;
            }
        }

        return null;
    }

    private String getTargetUri(HttpServletRequest request) {
        String targetUri = substringAfterLast(request.getRequestURI(), request.getContextPath());
        if (targetUri.endsWith("/")) {
            targetUri = substringBeforeLast(targetUri, "/");
        }
        targetUri = substringBeforeLast(targetUri, "/google-ds-api");
        targetUri = substringBeforeLast(targetUri, "/query");
        return targetUri;
    }

    private String[] getTableUris(String databaseUri, JdbcTemplate jdbcTemplate) {
        if (uriMappingsByDatasource.containsKey(databaseUri)) {
            String prepSql = "SELECT URI FROM " + uriMappingsByDatasource.get(databaseUri);
            return (String[]) jdbcTemplate.query(prepSql, new SingleStringResultSetExtractor());
        }

        ArrayList<String> tables = new ArrayList<String>();
        String[] values = (String[]) jdbcTemplate.execute(new DatabaseTablesConnectionCallback());
        if (values != null) {
            for (String value : values) {
                tables.add(databaseUri + "/" + value);
            }
        }
        return tables.toArray(new String[tables.size()]);
    }

    private String getTableName(String targetUri, String databaseUri, JdbcTemplate jdbcTemplate) throws ResourceNotFoundException {
        if (uriMappingsByDatasource.containsKey(databaseUri)) {
            String mappingsTable = uriMappingsByDatasource.get(databaseUri);

            String prepSql = "SELECT TABLE_NAME FROM " + mappingsTable + " WHERE URI = ? ";
            String[] tableNames = (String[]) jdbcTemplate.query(prepSql, new Object[]{targetUri}, new SingleStringResultSetExtractor());
            if (tableNames == null || tableNames.length == 0) {
                throw new ResourceNotFoundException(databaseUri);
            }

            // TODO : what if this returns more than one table?
            String tableName = tableNames[0];
            if (isEmpty(tableName)) {
                throw new ResourceNotFoundException(databaseUri);
            }
            return tableName;
        }

        return substringAfterLast(targetUri, "/");
    }
}
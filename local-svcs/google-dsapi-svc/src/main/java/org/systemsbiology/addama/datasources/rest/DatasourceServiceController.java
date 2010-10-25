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

import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.DataTableGenerator;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.datasources.datasource.JsonArrayDatasourceHelper;
import org.systemsbiology.addama.datasources.handlers.LocalsJsonConfigHandler;
import org.systemsbiology.addama.datasources.jdbctemplate.JdbcTemplateDataSource;
import org.systemsbiology.addama.datasources.jdbctemplate.SimpleSqlDataTableGenerator;
import org.systemsbiology.addama.datasources.jdbctemplate.SingleStringResultSetExtractor;
import org.systemsbiology.addama.datasources.rest.callbacks.DatabaseTableColumnConnectionCallback;
import org.systemsbiology.addama.datasources.rest.callbacks.DatabaseTablesConnectionCallback;
import org.systemsbiology.addama.datasources.util.TqxParser;
import org.systemsbiology.addama.registry.JsonConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class DatasourceServiceController implements InitializingBean {
    private static final Logger log = Logger.getLogger(DatasourceServiceController.class.getName());

    private final Map<String, JdbcTemplateDataSource> jdbcTemplateDsByDatabaseUri = new HashMap<String, JdbcTemplateDataSource>();
    private final Map<String, String> uriMappingsByDatasource = new HashMap<String, String>();

    private JsonConfig jsonConfig;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    /*
     * InitializingBean
     */

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new LocalsJsonConfigHandler(jdbcTemplateDsByDatabaseUri, uriMappingsByDatasource));
    }

    /*
     * Controllers
     */

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.info("processRequest(" + requestUri + ")");

        if (StringUtils.equalsIgnoreCase(requestUri, request.getContextPath())) {
            return getDatabases();
        }

        String databaseUri = getDatabaseUri(request);
        if (StringUtils.isEmpty(databaseUri)) {
            return getDatabases();
        }

        JdbcTemplateDataSource jdbcTemplateDs = jdbcTemplateDsByDatabaseUri.get(databaseUri);
        if (jdbcTemplateDs == null) {
            throw new ResourceNotFoundException(databaseUri);
        }

        if (requestUri.endsWith("/query") || requestUri.endsWith("/google-ds-api")) {
            String tableUri = getTargetUri(request);
            String tableName = getTableName(tableUri, jdbcTemplateDs);
            log.info("processRequest(" + requestUri + "):" + tableName);

            boolean isJsonArray = false;
            String tqx = request.getParameter("tqx");
            if (!StringUtils.isEmpty(tqx)) {
                TqxParser tqxParser = new TqxParser(tqx);
                isJsonArray = StringUtils.equalsIgnoreCase(tqxParser.getOut(), "json_array");
            }

            DataTableGenerator tableGenerator = new SimpleSqlDataTableGenerator(jdbcTemplateDs, tableName);
            if (isJsonArray) {
                JsonArrayDatasourceHelper.executeDataSourceServletFlow(request, response, tableGenerator);
            } else {
                DataSourceHelper.executeDataSourceServletFlow(request, response, tableGenerator, false);
            }
            return null;
        }

        String targetUri = getTargetUri(request);
        if (StringUtils.equals(targetUri, databaseUri)) {
            return getDatabase(jdbcTemplateDs);
        }

        if (StringUtils.equalsIgnoreCase(requestUri, databaseUri)) {
            return getDatabase(jdbcTemplateDs);
        }

        return getTable(targetUri, jdbcTemplateDs);
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

    protected ModelAndView getDatabase(JdbcTemplateDataSource dataSource) throws Exception {
        JSONObject json = new JSONObject();
        for (String tableUri : getTableUris(dataSource)) {
            JSONObject tableJson = new JSONObject();
            tableJson.put("name", StringUtils.substringAfterLast(tableUri, "/"));
            tableJson.put("uri", tableUri);
            json.append("items", tableJson);
        }

        ModelAndView mav = new ModelAndView(new JsonItemsView());
        mav.addObject("json", json);
        return mav;
    }

    protected ModelAndView getTable(String tableUri, JdbcTemplateDataSource dataSource) throws Exception {
        log.info("getTable(" + tableUri + ")");

        String tableName = getTableName(tableUri, dataSource);
        JdbcTemplate jdbcTemplate = dataSource.getJdbcTemplate();
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
        String targetUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        if (targetUri.endsWith("/")) {
            targetUri = StringUtils.substringBeforeLast(targetUri, "/");
        }
        targetUri = StringUtils.substringBeforeLast(targetUri, "/google-ds-api");
        targetUri = StringUtils.substringBeforeLast(targetUri, "/query");
        return targetUri;
    }

    private String[] getTableUris(JdbcTemplateDataSource dataSource) {
        String databaseUri = dataSource.getDatabaseUri();
        JdbcTemplate jdbcTemplate = dataSource.getJdbcTemplate();

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

    private String getTableName(String targetUri, JdbcTemplateDataSource dataSource) throws ResourceNotFoundException {
        String databaseUri = dataSource.getDatabaseUri();
        if (uriMappingsByDatasource.containsKey(databaseUri)) {
            String mappingsTable = uriMappingsByDatasource.get(databaseUri);

            JdbcTemplate jdbcTemplate = dataSource.getJdbcTemplate();
            String prepSql = "SELECT TABLE_NAME FROM " + mappingsTable + " WHERE URI = ? ";
            String[] tableNames = (String[]) jdbcTemplate.query(prepSql, new Object[]{targetUri}, new SingleStringResultSetExtractor());
            if (tableNames == null || tableNames.length == 0) {
                throw new ResourceNotFoundException(databaseUri);
            }

            // TODO : what if this returns more than one table?
            String tableName = tableNames[0];
            if (StringUtils.isEmpty(tableName)) {
                throw new ResourceNotFoundException(databaseUri);
            }
            return tableName;
        }

        return StringUtils.substringAfterLast(targetUri, "/");
    }
}
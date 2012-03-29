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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;
import org.systemsbiology.google.visualization.datasource.jdbc.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getURI;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow;

/**
 * @author hrovira
 */
@Controller
public class DatasourceServiceController implements InitializingBean {
    private final Map<String, String> uriMappingsById = new HashMap<String, String>();

    private final Map<String, JdbcTemplate> jdbcTemplateDsById = new HashMap<String, JdbcTemplate>();

    private ServiceConfig serviceConfig;

    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public void afterPropertiesSet() throws Exception {
        this.serviceConfig.visit(new JdbcTemplateMappingsHandler(jdbcTemplateDsById));
        this.serviceConfig.visit(new StringPropertyByIdMappingsHandler(uriMappingsById, "uriMappings"));
    }

    /*
    * Controllers
    */
    @RequestMapping(value = "/**/datasources", method = RequestMethod.GET)
    protected ModelAndView listDatabases(HttpServletRequest request) throws Exception {
        String baseuri = getURI(request);
        JSONObject json = new JSONObject();
        for (Mapping mapping : this.serviceConfig.getMappings()) {
            JSONObject item = new JSONObject();
            item.put("uri", baseuri + "/" + mapping.ID());
            item.put("label", mapping.LABEL());
            item.put("id", mapping.ID());
            json.append("items", item);
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/datasources/{databaseId}", method = RequestMethod.GET)
    protected ModelAndView listDatabase(HttpServletRequest request,
                                        @PathVariable("databaseId") String databaseId) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateDsById.get(databaseId);
        String baseUri = getURI(request);

        JSONObject json = new JSONObject();
        for (String tableId : getTableIds(databaseId, jdbcTemplate)) {
            JSONObject item = new JSONObject();
            item.put("name", tableId);
            item.put("uri", baseUri + "/" + tableId);
            json.append("items", item);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/datasources/{databaseId}/{tableId}", method = RequestMethod.GET)
    protected ModelAndView listTable(@PathVariable("databaseId") String databaseId,
                                     @PathVariable("tableId") String tableId) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateDsById.get(databaseId);
        String tableName = getRealTableName(tableId, databaseId, jdbcTemplate);
        JSONObject json = jdbcTemplate.execute(new DatabaseTableColumnConnectionCallback(tableName));
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/datasources/{databaseId}/{tableId}/query", method = RequestMethod.GET)
    protected void queryTable(HttpServletRequest request, HttpServletResponse response,
                              @PathVariable("databaseId") String databaseId,
                              @PathVariable("tableId") String tableId) throws Exception {
        JdbcTemplate jdbcTemplate = jdbcTemplateDsById.get(databaseId);
        String tableName = getRealTableName(tableId, databaseId, jdbcTemplate);

        SimpleSqlDataTableGenerator tableGenerator = new SimpleSqlDataTableGenerator(jdbcTemplate, tableName);
        executeDataSourceServletFlow(request, response, tableGenerator);
    }

    /*
     * Private Methods
     */

    private Iterable<String> getTableIds(String databaseId, JdbcTemplate jdbcTemplate) {
        if (uriMappingsById.containsKey(databaseId)) {
            String prepSql = "SELECT URI FROM " + uriMappingsById.get(databaseId);
            return jdbcTemplate.query(prepSql, new SingleStringResultSetExtractor());
        }

        return jdbcTemplate.execute(new DatabaseTablesConnectionCallback());
    }

    private String getRealTableName(String tableId, String databaseId, JdbcTemplate jdbcTemplate) throws ResourceNotFoundException {
        if (uriMappingsById.containsKey(databaseId)) {
            String mappingsTable = uriMappingsById.get(databaseId);

            String prepSql = "SELECT TABLE_NAME FROM " + mappingsTable + " WHERE URI = ? ";
            Iterable<String> tableNames = jdbcTemplate.query(prepSql, new Object[]{tableId}, new SingleStringResultSetExtractor());
            if (tableNames == null || !tableNames.iterator().hasNext()) {
                throw new ResourceNotFoundException(tableId);
            }

            // TODO : what if this returns more than one table?
            String tableName = tableNames.iterator().next();
            if (isEmpty(tableName)) {
                throw new ResourceNotFoundException(tableId);
            }
            return tableName;
        }

        return tableId;
    }
}
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
package org.systemsbiology.addama.filedbcreate.mvc;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.filedbcreate.etl.TableCreatorRunnable;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL.Types;
import org.systemsbiology.addama.filedbcreate.etl.ddl.LabeledDataMatrixTableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleConfigurationTableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleTableTableDDL;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.google.visualization.datasource.mvc.QueryController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getCleanUri;

/**
 * @author hrovira
 */
@Controller
public class DatasourceController extends QueryController {
    private static final Logger log = Logger.getLogger(DatasourceController.class.getName());

    protected final Map<String, String> rootPathsByUri = new HashMap<String, String>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        super.setJsonConfig(jsonConfig);
        jsonConfig.visit(new StringMapJsonConfigHandler(rootPathsByUri, "rootPath"));
    }

    @RequestMapping(method = RequestMethod.POST)
    protected ModelAndView post(HttpServletRequest request) throws Exception {
        JSONObject json = new JSONObject();

        Map<String, TableDDL> tableSchemasByUri = getTableSchemas(request);
        for (Map.Entry<String, TableDDL> entry : tableSchemasByUri.entrySet()) {
            uploadData(entry.getKey(), entry.getValue());
            json.append("items", new JSONObject().put("uri", entry.getKey()));
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
    * Protected Methods
    */

    protected void uploadData(String databaseUri, TableDDL tableDDL) throws Exception {
        log.info(databaseUri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(databaseUri);
        String tableName = "T_" + Math.abs(databaseUri.hashCode());
        String localPath = getRootPath(databaseUri) + databaseUri;

        Runnable r = new TableCreatorRunnable(jdbcTemplate, tableDDL, tableName, localPath);
        new Thread(r).start();

        log.info(databaseUri + ":" + tableName);
    }

    protected String getRootPath(String uri) {
        for (Map.Entry<String, String> entry : rootPathsByUri.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /*
    * Private Methods
    */

    private Map<String, TableDDL> getTableSchemas(HttpServletRequest request) throws Exception {
        HashMap<String, TableDDL> tableSchemas = new HashMap<String, TableDDL>();

        if (isMultipartContent(request)) {
            String requestUri = getCleanUri(request, "/datasources");
            ServletFileUpload fileUpload = new ServletFileUpload();
            FileItemIterator itemIterator = fileUpload.getItemIterator(request);
            while (itemIterator.hasNext()) {
                FileItemStream itemStream = itemIterator.next();
                if (!itemStream.isFormField()) {
                    JSONObject json = getJsonObject(itemStream);
                    TableDDL tableDDL = getTableSchema(json);
                    if (tableDDL != null) {
                        tableSchemas.put(requestUri + "/" + json.getString("filename"), tableDDL);
                    }
                }
            }
        }
        return tableSchemas;
    }

    private JSONObject getJsonObject(FileItemStream itemStream) {
        try {
            StringBuilder builder = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(itemStream.openStream()));
            String line = "";
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    builder.append(line);
                }
            }

            return new JSONObject(builder.toString());
        } catch (Exception e) {
            log.warning(itemStream.getName() + ":" + e);
        }
        return null;
    }

    private TableDDL getTableSchema(JSONObject json) {
        switch (Types.valueOf(json.optString("transform", Types.simpleTable.name()))) {
            case simpleTable:
                return new SimpleTableTableDDL();
            case labeledDataMatrix:
                return new LabeledDataMatrixTableDDL();
            case typeMap:
                return new SimpleConfigurationTableDDL(json);
        }
        return new SimpleTableTableDDL();
    }
}

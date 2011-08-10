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
package org.systemsbiology.addama.workspaces.jcr.rest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;
import org.systemsbiology.addama.filedbcreate.mvc.DatasourceController;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.workspaces.jcr.callbacks.JcrFileLocal;
import org.systemsbiology.addama.workspaces.jcr.callbacks.JcrTemplateJsonConfigHandler;
import org.systemsbiology.addama.workspaces.jcr.util.FromJcrTableCreatorRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class FromJcrDatasourceController extends DatasourceController {
    private static final Logger log = Logger.getLogger(FromJcrDatasourceController.class.getName());

    private final Map<String, JcrTemplate> jcrTemplatesByUri = new HashMap<String, JcrTemplate>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        super.setJsonConfig(jsonConfig);
        jsonConfig.visit(new JcrTemplateJsonConfigHandler(jcrTemplatesByUri));
    }

    /*
    * Protected Methods
    */

    protected void uploadData(String databaseUri, TableDDL tableDDL) throws Exception {
        log.info(databaseUri);

        JdbcTemplate jdbcTemplate = getJdbcTemplate(databaseUri);
        String tableName = "T_" + Math.abs(databaseUri.hashCode());
        String localPath = getRootPath(databaseUri) + databaseUri;

        Runnable r = new FromJcrTableCreatorRunnable(jdbcTemplate, tableDDL, tableName, getFileLocal(databaseUri, localPath));
        new Thread(r).start();

        log.info(databaseUri + ":" + tableName);
    }

    protected JcrFileLocal getFileLocal(String databaseUri, String localPath) {
        for (Map.Entry<String, JcrTemplate> entry : jcrTemplatesByUri.entrySet()) {
            if (databaseUri.startsWith(entry.getKey())) {
                return new JcrFileLocal(entry.getValue(), localPath, databaseUri);
            }
        }
        return null;
    }


}

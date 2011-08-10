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
package org.systemsbiology.addama.filedbcreate.etl.ddl;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class SimpleConfigurationTableDDL implements TableDDL {
    private static final Logger log = Logger.getLogger(SimpleConfigurationTableDDL.class.getName());

    private final JSONObject simpleConfig;

    public SimpleConfigurationTableDDL(JSONObject simpleConfig) {
        this.simpleConfig = simpleConfig;
    }

    public String getStatement(String tableName, String[] columnHeaders) {
        StringBuilder builder = new StringBuilder();
        builder.append("create table ").append(tableName).append(" (");
        for (int i = 0; i < columnHeaders.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            String columnHeader = columnHeaders[i];
            builder.append(columnHeader).append(" ").append(getDataType(columnHeader));
        }
        builder.append(" );");
        return builder.toString();
    }

    public String[] getIndexStatement(String table, String[] columnHeaders) {
        ArrayList<String> indexSql = new ArrayList<String>();
        for (int i = 0; i < columnHeaders.length; i++) {
            String columnHeader = columnHeaders[i];
            String dataType = getDataType(columnHeader);
            if (StringUtils.equalsIgnoreCase(dataType, "text")) {
                indexSql.add("create index idx_" + table + "_" + i + " on " + table + " ( " + columnHeader + "(10) )");
            } else {
                indexSql.add("create index idx_" + table + "_" + i + " on " + table + " ( " + columnHeader + " )");
            }
        }
        return indexSql.toArray(new String[indexSql.size()]);
    }

    public Map<String, String> getDataTypes(String[] columnHeaders) {
        HashMap<String, String> dataTypes = new HashMap<String, String>();
        for (String columnHeader : columnHeaders) {
            dataTypes.put(columnHeader, getDataType(columnHeader));
        }
        return dataTypes;
    }

    /*
    * Private Methods
    */

    private String getDataType(String columnHeader) {
        if (simpleConfig.has(columnHeader)) {
            try {
                return simpleConfig.getString(columnHeader);
            } catch (JSONException e) {
                log.warning("getDataType(" + columnHeader + "):" + e);
            }
        }
        return "text";
    }
}

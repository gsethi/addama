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

import org.systemsbiology.addama.filedbcreate.etl.TableDDL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hrovira
 */
public class LabeledDataMatrixTableDDL implements TableDDL {

    public String getStatement(String tableName, String[] columnHeaders) {
        StringBuilder builder = new StringBuilder();
        builder.append("create table ").append(tableName).append(" (");
        builder.append(columnHeaders[0]).append(" text");
        for (int t = 1; t < columnHeaders.length; t++) {
            builder.append(", ").append(columnHeaders[t]).append(" double");
        }
        builder.append(" );");
        return builder.toString();
    }

    public String[] getIndexStatement(String table, String[] columnHeaders) {
        ArrayList<String> indexSql = new ArrayList<String>();
        indexSql.add("create index idx_" + table + "_0 on " + table + " ( " + columnHeaders[0] + "(10) )");
        for (int i = 1; i < columnHeaders.length; i++) {
            indexSql.add("create index idx_" + table + "_" + i + " on " + table + " ( " + columnHeaders[i] + " )");
        }
        return indexSql.toArray(new String[indexSql.size()]);
    }

    public Map<String, String> getDataTypes(String[] columnHeaders) {
        HashMap<String, String> dataTypes = new HashMap<String, String>();
        dataTypes.put(columnHeaders[0], "text");
        for (int i = 1; i < columnHeaders.length; i++) {
            dataTypes.put(columnHeaders[i], "double");
        }
        return dataTypes;
    }
}

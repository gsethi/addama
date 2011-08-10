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

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;

/**
 * @author hrovira
 */
public class CreateIndexesConnectionCallback implements ConnectionCallback {
    private static final Logger log = Logger.getLogger(CreateIndexesConnectionCallback.class.getName());

    private final TableDDL tableDDL;
    private final String tableName;
    private final String[] columnHeaders;
    private int maxNumberOfIndexes = MAX_VALUE;

    public CreateIndexesConnectionCallback(TableDDL tableDDL, String tableName, String[] columnHeaders) {
        this.tableDDL = tableDDL;
        this.tableName = tableName;
        this.columnHeaders = columnHeaders;
    }

    public CreateIndexesConnectionCallback(TableDDL tableDDL, String tableName, String[] columnHeaders, int maxIndexes) {
        this(tableDDL, tableName, columnHeaders);
        this.setMaxNumberOfIndexes(maxIndexes);
    }

    public void setMaxNumberOfIndexes(int maxNumberOfIndexes) {
        this.maxNumberOfIndexes = maxNumberOfIndexes;
    }

    public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
        String[] indexSqls = tableDDL.getIndexStatement(tableName, columnHeaders);
        log.info("creating indexes:" + indexSqls.length);

        Statement stmt = connection.createStatement();
        for (int i = 0; i < indexSqls.length; i++) {
            if (i < maxNumberOfIndexes) {
                String indexSql = indexSqls[i];
                log.info("indexSql=" + indexSql);
                stmt.execute(indexSql);
            } else {
                log.warning("exceeded max number of indexes:" + i);
                break;
            }
        }

        log.info("completed");
        connection.commit();
        connection.close();
        return null;
    }

}
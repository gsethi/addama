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

/**
 * @author hrovira
 */
public class CreateTableConnectionCallback implements ConnectionCallback {
    private static final Logger log = Logger.getLogger(CreateTableConnectionCallback.class.getName());

    private final TableDDL tableDDL;
    private final String tableName;
    private final String[] columnHeaders;

    public CreateTableConnectionCallback(TableDDL tableDDL, String tableName, String[] columnHeaders) {
        this.tableDDL = tableDDL;
        this.tableName = tableName;
        this.columnHeaders = columnHeaders;
    }

    public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
        String tableSql = tableDDL.getStatement(tableName, columnHeaders);
        log.info("creating table:" + tableSql);

        Statement stmt = connection.createStatement();
        stmt.execute(tableSql);

        log.info("completed");
        connection.commit();
        connection.close();
        return null;
    }

}
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

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class BatchInsertConnectionCallback implements ConnectionCallback {
    private static final Logger log = Logger.getLogger(BatchInsertConnectionCallback.class.getName());

    private final BufferedReader bufferedReader;
    private final String separator;
    private final String tableName;
    private final String[] columnHeaders;

    public BatchInsertConnectionCallback(BufferedReader reader, String separator, String tableName, String[] columnHeaders) {
        this.bufferedReader = reader;
        this.separator = separator;
        this.tableName = tableName;
        this.columnHeaders = columnHeaders;
    }

    public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
        try {
            log.info("doInConnection");
            connection.setAutoCommit(false);

            PreparedStatement ps = connection.prepareStatement(getInsertSql(columnHeaders));

            int currentBatch = 0;
            String line = "";
            while (line != null) {
                line = bufferedReader.readLine();
                if (line != null) {
                    String[] values = line.split(separator);
                    for (int i = 0; i < values.length; i++) {
                        ps.setString(i + 1, values[i]);
                    }
                    ps.addBatch();
                    if (++currentBatch % 1000 == 0) {
                        ps.executeBatch();
                        log.info("execute batch:" + tableName + ":" + currentBatch);
                    }
                }
            }

            ps.executeBatch();
            log.info("execute batch:" + tableName + ":" + currentBatch);

            connection.commit();
            connection.close();

            log.info("commit and close");
            return null;
        } catch (IOException e) {
            throw new SQLException("problems reading file", e);
        }
    }

    /*
    * Private Methods
    */

    private String getInsertSql(String[] columnHeaders) {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into ").append(tableName).append(" (");
        for (int q = 0; q < columnHeaders.length; q++) {
            if (q > 0) {
                builder.append(",");
            }
            builder.append(columnHeaders[q]);
        }
        builder.append(") values (");
        for (int q = 0; q < columnHeaders.length; q++) {
            if (q > 0) {
                builder.append(",");
            }
            builder.append("?");
        }
        builder.append(")");
        return builder.toString();
    }
}
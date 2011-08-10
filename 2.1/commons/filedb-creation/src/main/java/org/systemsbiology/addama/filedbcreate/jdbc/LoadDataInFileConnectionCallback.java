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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class LoadDataInFileConnectionCallback implements ConnectionCallback {
    private static final Logger log = Logger.getLogger(LoadDataInFileConnectionCallback.class.getName());
    private final String filePath;
    private final String tableName;
    private String separator = "\t";

    public LoadDataInFileConnectionCallback(String filePath, String tableName) {
        this.filePath = filePath;
        this.tableName = tableName;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
        log.info("loading data local infile from " + filePath + " to " + tableName);

        String sql = "LOAD DATA LOCAL INFILE '" + filePath + "' INTO TABLE " + tableName +
                " FIELDS TERMINATED BY '" + separator + "' LINES TERMINATED BY '\n' IGNORE 1 LINES;";

        Statement stmt = connection.createStatement();
        stmt.execute(sql);
        stmt.close();

        log.info("completed uploading " + tableName);
        return null;
    }
}

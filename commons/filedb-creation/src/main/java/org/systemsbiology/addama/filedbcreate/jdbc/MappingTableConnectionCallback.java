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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author hrovira
 */
public class MappingTableConnectionCallback implements ConnectionCallback {
    private final String uri;
    private final String tableName;

    public MappingTableConnectionCallback(String uri, String tableName) {
        this.uri = uri;
        this.tableName = tableName;
    }

    public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS T_URI_TABLE_MAPPINGS (URI TEXT, TABLE_NAME TEXT);");

        PreparedStatement ps = conn.prepareStatement("INSERT INTO T_URI_TABLE_MAPPINGS (URI, TABLE_NAME) VALUES (?,?)");
        ps.setString(1, uri);
        ps.setString(2, tableName);
        ps.execute();

        conn.commit();
        conn.close();
        return null;
    }
}

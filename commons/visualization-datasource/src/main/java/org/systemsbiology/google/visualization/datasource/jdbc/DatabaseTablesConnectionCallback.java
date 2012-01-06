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
package org.systemsbiology.google.visualization.datasource.jdbc;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author hrovira
 */
public class DatabaseTablesConnectionCallback implements ConnectionCallback {

    public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
        ArrayList<String> tableNames = new ArrayList<String>();

        String[] types = new String[]{"TABLE", "VIEW"};
        ResultSet rs = connection.getMetaData().getTables(null, null, "%", types);
        while (rs.next()) {
            tableNames.add(rs.getString(3));
        }

        return tableNames.toArray(new String[tableNames.size()]);
    }

}
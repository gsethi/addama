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
package org.systemsbiology.addama.datasources.jdbctemplate;

import com.google.visualization.datasource.Capabilities;
import com.google.visualization.datasource.DataTableGenerator;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.query.Query;

import javax.servlet.http.HttpServletRequest;

/**
 * @author hrovira
 */
public class SimpleSqlDataTableGenerator implements DataTableGenerator {
    private final JdbcTemplateDataSource dataSource;
    private final String tableName;

    public SimpleSqlDataTableGenerator(JdbcTemplateDataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
    }

    /*
     * DataTableGenerator
     */

    public DataTable generateDataTable(Query query, HttpServletRequest request) throws DataSourceException {
        return dataSource.executeQuery(query, tableName);
    }

    public Capabilities getCapabilities() {
        return Capabilities.SQL;
    }
}
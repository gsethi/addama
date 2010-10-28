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
package org.systemsbiology.addama.services.execution.datasource;

import com.google.visualization.datasource.DataTableGenerator;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.util.CsvDataSourceHelper;
import com.ibm.icu.util.ULocale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author hrovira
 */
public class CsvFileDataTableGenerator extends AbstractDataTableGenerator implements DataTableGenerator {

    public CsvFileDataTableGenerator(InputStream inputStream, String line) {
        super(inputStream, line, ",");
    }

    protected DataTable generateDataTable(BufferedReader reader, ULocale requestLocale,
                                          List<ColumnDescription> columns)
            throws IOException, DataSourceException {
        return CsvDataSourceHelper.read(reader, columns, !columns.isEmpty(), requestLocale);
    }
}
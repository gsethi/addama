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
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.ValueFormatter;
import com.google.visualization.datasource.datatable.value.ValueType;
import com.ibm.icu.util.ULocale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class TsvFileDataTableGenerator extends AbstractDataTableGenerator implements DataTableGenerator {
    private static final Logger log = Logger.getLogger(TsvFileDataTableGenerator.class.getName());

    public TsvFileDataTableGenerator(InputStream inputStream, String line) {
        super(inputStream, line, "\t");
    }

    protected DataTable generateDataTable(BufferedReader reader, ULocale requestLocale,
                                          List<ColumnDescription> columnDescriptions)
            throws IOException, DataSourceException {
        DataTable dataTable = new DataTable();
        dataTable.addColumns(columnDescriptions);

        ValueFormatter valueFormatter = ValueFormatter.createDefault(ValueType.TEXT, requestLocale);

        // Parse the TSV.
        String line = reader.readLine(); // skip headers
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                try {
                    TableRow tableRow = new TableRow();

                    String[] columnValues = line.split("\t");
                    for (String value : columnValues) {
                        if (value != null) {
                            value = value.trim();
                        }
                        tableRow.addCell(valueFormatter.parse(value));
                    }

                    dataTable.addRow(tableRow);
                } catch (TypeMismatchException e) {
                    log.warning("read(): " + e);
                }
            }
        }

        return dataTable;
    }
}

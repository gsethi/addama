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

import com.google.visualization.datasource.Capabilities;
import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.DataTableGenerator;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.value.ValueType;
import com.google.visualization.datasource.query.Query;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public abstract class AbstractDataTableGenerator implements DataTableGenerator {
    private static final Logger log = Logger.getLogger(AbstractDataTableGenerator.class.getName());

    private final InputStream inputStream;
    private final ArrayList<ColumnDescription> columnDescriptions = new ArrayList<ColumnDescription>();

    protected AbstractDataTableGenerator(InputStream inputStream, String line, String separator) {
        this.inputStream = inputStream;

        String headerLine = StringUtils.replace(line, "\"", "");
        if (headerLine.contains(separator)) {
            String[] columnHeaders = headerLine.split(separator);
            for (String columnHeader : columnHeaders) {
                columnDescriptions.add(new ColumnDescription(columnHeader, ValueType.TEXT, columnHeader));
            }
        }
    }

    /*
     * DataTableGenerator 
     */

    public DataTable generateDataTable(Query query, HttpServletRequest request) throws DataSourceException {
        ULocale requestLocale = DataSourceHelper.getLocaleFromRequest(request);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            return generateDataTable(reader, requestLocale, columnDescriptions);
        } catch (IOException e) {
            log.warning("generateDataTable():" + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warning("generateDataTable():" + e);
                }
            }
        }
        return null;
    }

    public Capabilities getCapabilities() {
        return Capabilities.NONE;
    }

    /*
     * Protected Methods
     */

    protected abstract DataTable generateDataTable(BufferedReader reader, ULocale requestLocale,
                                                   List<ColumnDescription> columns)
            throws IOException, DataSourceException;
}
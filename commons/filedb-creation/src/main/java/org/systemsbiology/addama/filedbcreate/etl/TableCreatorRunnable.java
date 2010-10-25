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
package org.systemsbiology.addama.filedbcreate.etl;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.filedbcreate.jdbc.BatchInsertConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.CreateTableConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.DropTableConnectionCallback;

import java.io.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class TableCreatorRunnable implements Runnable {
    private static final Logger log = Logger.getLogger(TableCreatorRunnable.class.getName());

    private final JdbcTemplate jdbcTemplate;
    private final String filename;
    private final String tableName;
    private final TableDDL tableDDL;
    private String separator = "\t";

    public TableCreatorRunnable(JdbcTemplate jdbcTemplate, String filename, String tableName, TableDDL tableDDL) throws IOException {
        this.jdbcTemplate = jdbcTemplate;
        this.filename = filename;
        this.tableName = tableName;
        this.tableDDL = tableDDL;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void run() {
        try {
            jdbcTemplate.execute(new DropTableConnectionCallback(tableName));
        } catch (Exception e) {
            log.info("dropTable:" + tableName + ":" + e);
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String header = reader.readLine();

            String[] columnHeaders = clean(header).split(separator);

            jdbcTemplate.execute(new CreateTableConnectionCallback(tableDDL, tableName, columnHeaders));
            jdbcTemplate.execute(new BatchInsertConnectionCallback(reader, separator, tableName, columnHeaders));

            log.info("completed uploading " + tableName);

            File f = new File(filename);
            f.delete();
            log.info("deleted " + filename);
        } catch (Exception e) {
            log.warning("errored on " + tableName + ":" + e);
        }
    }

    private String clean(String value) {
        return StringUtils.replace(value, "-", "_");
    }
}

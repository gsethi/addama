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
import org.systemsbiology.addama.filedbcreate.jdbc.CreateIndexesConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.CreateTableConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.DropTableConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.LoadDataInFileConnectionCallback;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class TableCreatorRunnable implements Runnable {
    private static final Logger log = Logger.getLogger(TableCreatorRunnable.class.getName());

    private final JdbcTemplate jdbcTemplate;
    private final String localFile;
    private final String tableName;
    private final TableDDL tableDDL;
    private String separator = "\t";

    /*
     * Constructors
     */

    public TableCreatorRunnable(JdbcTemplate jdbcTemplate, TableDDL tableDDL, String tableName, String localFile) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableDDL = tableDDL;
        this.tableName = tableName;
        this.localFile = localFile;
    }

    /*
     * Dependency Injection
     */

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /*
     * Runnable
     */

    public void run() {
        try {
            jdbcTemplate.execute(new DropTableConnectionCallback(tableName));
        } catch (Exception e) {
            log.info("dropTable:" + tableName + ":" + e);
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(localFile)));
            String[] columnHeaders = clean(reader.readLine()).split(separator);

            jdbcTemplate.execute(new CreateTableConnectionCallback(tableDDL, tableName, columnHeaders));
            jdbcTemplate.execute(new CreateIndexesConnectionCallback(tableDDL, tableName, columnHeaders));
            jdbcTemplate.execute(new LoadDataInFileConnectionCallback(localFile, tableName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                log.warning("unable to close:" + e);
            }
        }
    }

    /*
    * Private Methods
    */

    private String clean(String value) {
        return StringUtils.replace(value, "-", "_");
    }

}
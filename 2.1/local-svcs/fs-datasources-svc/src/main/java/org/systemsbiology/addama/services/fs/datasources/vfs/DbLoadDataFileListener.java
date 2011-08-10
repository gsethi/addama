package org.systemsbiology.addama.services.fs.datasources.vfs;

import org.apache.commons.vfs.FileChangeEvent;
import org.apache.commons.vfs.FileListener;
import org.apache.commons.vfs.FileObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.filedbcreate.ColumnBean;
import org.systemsbiology.addama.filedbcreate.TableBean;
import org.systemsbiology.addama.filedbcreate.etl.TableDDL;
import org.systemsbiology.addama.filedbcreate.etl.ddl.SimpleTableTableDDL;
import org.systemsbiology.addama.filedbcreate.jdbc.CreateIndexesConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.CreateTableConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.DropTableConnectionCallback;
import org.systemsbiology.addama.filedbcreate.jdbc.LoadDataInFileConnectionCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.filedbcreate.dao.UriTableMappings.deleteMapping;
import static org.systemsbiology.addama.filedbcreate.dao.UriTableMappings.insertMapping;
import static org.systemsbiology.addama.services.fs.datasources.jdbc.TableLookupResultSetExtractor.findTables;

/**
 * @author hrovira
 */
public class DbLoadDataFileListener implements FileListener {
    private static final Logger log = Logger.getLogger(DbLoadDataFileListener.class.getName());

    private final String rootPath;
    private final JdbcTemplate jdbcTemplate;
    private Integer maxIndexes = 60;
    private TableDDL tableDDL = new SimpleTableTableDDL();

    public DbLoadDataFileListener(String rootPath, JdbcTemplate jdbcTemplate) {
        this.rootPath = rootPath;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setMaxIndexes(Integer maxIndexes) {
        this.maxIndexes = maxIndexes;
    }

    public void setTableDDL(TableDDL tableDDL) {
        this.tableDDL = tableDDL;
    }

    /*
    * FileListener
    */

    public void fileCreated(FileChangeEvent event) throws Exception {
        this.fileDeleted(event);

        FileObject fileObject = event.getFile();
        String tableUri = getTableUri(fileObject);
        try {
            if (isFile(fileObject)) {
                TableBean newTable = new TableBean(tableUri, "T_" + replace(randomUUID().toString(), "-", ""));

                String filepath = fileObject.getName().getPath();
                String tableName = newTable.getTableName();
                ColumnBean[] columns = getColumns(fileObject, newTable, tableDDL);
                String[] columnHeaders = getColumnHeaders(columns);

                insertMapping(jdbcTemplate, newTable, columns);

                jdbcTemplate.execute(new CreateTableConnectionCallback(tableDDL, tableName, columnHeaders));
                jdbcTemplate.execute(new CreateIndexesConnectionCallback(tableDDL, tableName, columnHeaders, maxIndexes));
                jdbcTemplate.execute(new LoadDataInFileConnectionCallback(filepath, newTable.getTableName()));
            }
        } catch (Exception e) {
            log.warning(tableUri + ":" + e);
        }
    }

    public void fileDeleted(FileChangeEvent event) throws Exception {
        String tableUri = getTableUri(event.getFile());
        TableBean[] tables = findTables(tableUri, jdbcTemplate);
        try {
            for (TableBean tb : tables) {
                log.info("dropping:" + tb.getTableName());
                jdbcTemplate.execute(new DropTableConnectionCallback(tb.getTableName()));
            }
        } catch (Exception e) {
            log.warning(tableUri + ":" + e);
        }
        try {
            deleteMapping(jdbcTemplate, tables);
        } catch (Exception e) {
            log.warning(tableUri + ":" + e);
        }
    }

    public void fileChanged(FileChangeEvent event) throws Exception {
        this.fileCreated(event);
    }

    /*
     * Private Methods
     */

    private boolean isFile(FileObject fileObject) {
        String filepath = fileObject.getName().getPath();
        log.info("path:" + filepath);

        if (new File(filepath).isFile()) {
            return true;
        }

        log.info("not a file, skipping:" + filepath);
        return false;
    }

    private ColumnBean[] getColumns(FileObject fileObject, TableBean table, TableDDL tableDDL) throws Exception {
        InputStream inputStream = fileObject.getContent().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String firstLine = reader.readLine();
            String separator = detectSeparator(firstLine);
            if (isEmpty(separator)) {
                throw new Exception("may not be a queryable file:" + fileObject.getName().getPath());
            }

            String[] columns = firstLine.split(separator);
            Map<String, String> types = tableDDL.getDataTypes(columns);

            ColumnBean[] columnBeans = new ColumnBean[columns.length];
            for (int i = 0; i < columns.length; i++) {
                String column = columns[i];
                String datatype = types.get(column);
                log.info("column:" + i + ":" + column + ":" + datatype);
                columnBeans[i] = new ColumnBean(table, "C_" + i, column, datatype);
            }
            return columnBeans;
        } finally {
            reader.close();
        }
    }

    private String[] getColumnHeaders(ColumnBean[] columnBeans) throws Exception {
        ArrayList<String> columns = new ArrayList<String>();
        for (ColumnBean columnBean : columnBeans) {
            columns.add(columnBean.getName());
        }
        return columns.toArray(new String[columns.size()]);
    }

    private String detectSeparator(String firstLine) {
        if (firstLine.contains("\t")) {
            return "\t";
        }
        if (firstLine.contains(",")) {
            return ",";
        }
        return null;
    }

    private String getTableUri(FileObject fileObject) {
        String filepath = fileObject.getName().getPath();
        String tableUri = substringAfterLast(filepath, rootPath);
        return chomp(tableUri, "/");
    }
}

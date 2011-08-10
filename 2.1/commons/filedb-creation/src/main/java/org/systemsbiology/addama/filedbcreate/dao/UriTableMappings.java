package org.systemsbiology.addama.filedbcreate.dao;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.filedbcreate.ColumnBean;
import org.systemsbiology.addama.filedbcreate.TableBean;
import org.systemsbiology.addama.filedbcreate.jdbc.ColumnBeansBatchPreparedStatementSetter;
import org.systemsbiology.addama.filedbcreate.jdbc.DeleteTablesBatchPreparedStatementSetter;

/**
 * @author hrovira
 */
public class UriTableMappings {

    public static void createTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS T_URI_TABLE_MAPPINGS (URI TEXT, TABLE_NAME TEXT);");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS T_URI_COLUMN_MAPPINGS (TABLE_URI TEXT, COLUMN_NAME TEXT, COLUMN_LABEL TEXT, DATA_TYPE TEXT);");
    }

    public static void insertMapping(JdbcTemplate jdbcTemplate, String uri, String table) {
        jdbcTemplate.update("INSERT INTO T_URI_TABLE_MAPPINGS (URI, TABLE_NAME) VALUES (?,?)", new String[]{uri, table});
    }

    public static void insertMapping(JdbcTemplate jdbcTemplate, TableBean table, ColumnBean... columns) {
        String[] tableArgs = new String[]{table.getUri(), table.getTableName()};
        jdbcTemplate.update("INSERT INTO T_URI_TABLE_MAPPINGS (URI, TABLE_NAME) VALUES (?,?)", tableArgs);
        if (columns != null) {
            String sql = "INSERT INTO T_URI_COLUMN_MAPPINGS (TABLE_URI, COLUMN_NAME, COLUMN_LABEL, DATA_TYPE) VALUES (?,?,?,?)";
            jdbcTemplate.batchUpdate(sql, new ColumnBeansBatchPreparedStatementSetter(columns));
        }
    }

    public static void deleteMapping(JdbcTemplate jdbcTemplate, TableBean... tables) {
        BatchPreparedStatementSetter setter = new DeleteTablesBatchPreparedStatementSetter(tables);
        jdbcTemplate.batchUpdate("DELETE FROM T_URI_TABLE_MAPPINGS WHERE URI = ?", setter);
        jdbcTemplate.batchUpdate("DELETE FROM T_URI_COLUMN_MAPPINGS WHERE TABLE_URI = ?", setter);
    }

    public static Object queryByUri(JdbcTemplate jdbcTemplate, String uri, ResultSetExtractor extractor) {
        String[] args = new String[]{uri};
        return jdbcTemplate.query("SELECT * FROM T_URI_TABLE_MAPPINGS WHERE URI = ?", args, extractor);
    }

    public static Object matchByUri(JdbcTemplate jdbcTemplate, String uri, ResultSetExtractor extractor) {
        String[] args = new String[]{uri + "%"};
        return jdbcTemplate.query("SELECT * FROM T_URI_TABLE_MAPPINGS WHERE URI LIKE ?", args, extractor);
    }

    public static Object columnsByTableUri(JdbcTemplate jdbcTemplate, TableBean table, ResultSetExtractor extractor) {
        String[] args = new String[]{table.getUri()};
        return jdbcTemplate.query("SELECT * FROM T_URI_COLUMN_MAPPINGS WHERE TABLE_URI = ?", args, extractor);
    }

}

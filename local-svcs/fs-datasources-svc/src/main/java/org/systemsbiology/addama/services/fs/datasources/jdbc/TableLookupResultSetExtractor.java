package org.systemsbiology.addama.services.fs.datasources.jdbc;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.filedbcreate.TableBean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.systemsbiology.addama.filedbcreate.dao.UriTableMappings.queryByUri;

/**
 * @author hrovira
 */
public class TableLookupResultSetExtractor implements ResultSetExtractor {
    /**
     * Extracts table information from resultset
     *
     * @param rs - resultset
     * @return object - array of TableBean
     * @throws SQLException
     * @throws DataAccessException
     */
    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        ArrayList<TableBean> tables = new ArrayList<TableBean>();
        while (rs.next()) {
            tables.add(new TableBean(rs.getString("URI"), rs.getString("TABLE_NAME")));
        }
        return tables.toArray(new TableBean[tables.size()]);
    }

    /**
     * Retrieves Table information from database
     *
     * @param tableUri     - target uri for table
     * @param jdbcTemplate - used to access database
     * @return tablebean array
     */
    public static TableBean[] findTables(String tableUri, JdbcTemplate jdbcTemplate) {
        TableLookupResultSetExtractor extractor = new TableLookupResultSetExtractor();
        TableBean[] tables = (TableBean[]) queryByUri(jdbcTemplate, tableUri, extractor);
        if (tables == null) {
            return new TableBean[0];
        }
        return tables;
    }

}

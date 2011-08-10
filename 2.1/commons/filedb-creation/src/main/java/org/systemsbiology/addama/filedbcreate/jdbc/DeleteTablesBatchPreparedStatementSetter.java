package org.systemsbiology.addama.filedbcreate.jdbc;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.systemsbiology.addama.filedbcreate.TableBean;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class DeleteTablesBatchPreparedStatementSetter implements BatchPreparedStatementSetter {
    private final TableBean[] tables;

    public DeleteTablesBatchPreparedStatementSetter(TableBean... tables) {
        this.tables = tables;
    }

    public void setValues(PreparedStatement ps, int i) throws SQLException {
        ps.setString(1, tables[i].getUri());
    }

    public int getBatchSize() {
        return tables.length;
    }
}

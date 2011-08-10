package org.systemsbiology.addama.filedbcreate.jdbc;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.systemsbiology.addama.filedbcreate.ColumnBean;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class ColumnBeansBatchPreparedStatementSetter implements BatchPreparedStatementSetter {
    private final ColumnBean[] columnBeans;

    public ColumnBeansBatchPreparedStatementSetter(ColumnBean[] columnBeans) {
        this.columnBeans = columnBeans;
    }

    public void setValues(PreparedStatement ps, int i) throws SQLException {
        ColumnBean column = columnBeans[i];
        ps.setString(1, column.getTableUri());
        ps.setString(2, column.getName());
        ps.setString(3, column.getLabel());
        ps.setString(4, column.getType());
    }

    public int getBatchSize() {
        return this.columnBeans.length;
    }
}

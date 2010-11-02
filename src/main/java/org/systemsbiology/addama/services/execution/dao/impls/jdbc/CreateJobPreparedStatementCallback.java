package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.systemsbiology.addama.services.execution.dao.Job;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class CreateJobPreparedStatementCallback implements PreparedStatementCallback {
    private final Job job;

    public CreateJobPreparedStatementCallback(Job job) {
        this.job = job;
    }

    public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
        Connection c = ps.getConnection();
        c.setAutoCommit(true);
        
        ps.setString(1, job.getJobUri());
        ps.setString(2, job.getScriptUri());
        ps.setString(3, job.getUserUri());
        ps.setString(4, job.getJobDirectory());
        ps.setString(5, job.getInputs().toString());

        ps.setString(6, job.getLabel());
        ps.setString(7, job.getJobStatus().name());
        ps.setString(8, job.getExecutionDirectory());
        ps.setString(9, job.getErrorMessage());

        ps.setDate(10, new Date(job.getCreatedAt().getTime()));
        ps.setDate(11, new Date(job.getModifiedAt().getTime()));

        return null;
    }
}

package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.systemsbiology.addama.services.execution.dao.Job;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
* @author hrovira
*/
public class UpdateJobPreparedStatementCallback implements PreparedStatementCallback {
    private final Job job;

    public UpdateJobPreparedStatementCallback(Job job) {
        this.job = job;
    }

    public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
        ps.setString(1, job.getJobStatus().name());
        ps.setString(2, job.getErrorMessage());
        ps.setDate(3, new Date(job.getModifiedAt().getTime()));
        ps.setString(4, job.getJobUri());

        return null;
    }
}

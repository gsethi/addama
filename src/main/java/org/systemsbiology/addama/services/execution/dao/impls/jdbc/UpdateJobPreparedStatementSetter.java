package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.systemsbiology.addama.services.execution.dao.Job;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class UpdateJobPreparedStatementSetter implements PreparedStatementSetter {
    private final Job job;

    public UpdateJobPreparedStatementSetter(Job job) {
        this.job = job;
    }

    public void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, job.getJobStatus().name());
        ps.setString(2, job.getErrorMessage());
        ps.setDate(3, new Date(job.getModifiedAt().getTime()));
        ps.setString(4, job.getJobUri());
    }
}

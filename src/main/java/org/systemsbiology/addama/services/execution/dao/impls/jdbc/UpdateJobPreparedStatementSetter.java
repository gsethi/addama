package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.systemsbiology.addama.services.execution.dao.Job;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

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
        ps.setTimestamp(3, new Timestamp(job.getModifiedAt().getTime()));
        ps.setString(4, job.getJobUri());
    }
}

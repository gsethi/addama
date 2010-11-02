package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.systemsbiology.addama.services.execution.dao.Job;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author hrovira
 */
public class CreateJobPreparedStatementSetter implements PreparedStatementSetter {
    private final Job job;

    public CreateJobPreparedStatementSetter(Job job) {
        this.job = job;
    }

    public void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, job.getJobUri());
        ps.setString(2, job.getScriptUri());
        ps.setString(3, job.getUserUri());
        ps.setString(4, job.getJobDirectory());
        ps.setString(5, job.getInputs().toString());

        ps.setString(6, job.getLabel());
        ps.setString(7, job.getJobStatus().name());
        ps.setString(8, job.getExecutionDirectory());
        ps.setString(9, job.getErrorMessage());

        ps.setTimestamp(10, new Timestamp(job.getCreatedAt().getTime()));
        ps.setTimestamp(11, new Timestamp(job.getModifiedAt().getTime()));
    }
}

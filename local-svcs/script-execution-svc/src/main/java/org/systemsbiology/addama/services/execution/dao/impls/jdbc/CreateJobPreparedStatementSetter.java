package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.systemsbiology.addama.services.execution.jobs.Job;

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
        int position = 0;
        ps.setString(++position, job.getJobId());
        ps.setString(++position, job.getScriptUri());
        ps.setString(++position, job.getUserUri());
        ps.setString(++position, job.getJobDirectory());
        ps.setString(++position, job.getLabel());
        ps.setString(++position, job.getJobStatus().name());
        ps.setString(++position, job.getExecutionDirectory());
        ps.setString(++position, job.getErrorMessage());
        ps.setString(++position, job.getScriptPath());
        ps.setString(++position, job.getScriptArgs());
        ps.setString(++position, job.getChannelUri());

        ps.setTimestamp(++position, new Timestamp(job.getCreatedAt().getTime()));
        ps.setTimestamp(++position, new Timestamp(job.getModifiedAt().getTime()));
    }
}

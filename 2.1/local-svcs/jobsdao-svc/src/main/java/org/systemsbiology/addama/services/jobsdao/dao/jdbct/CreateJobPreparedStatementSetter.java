package org.systemsbiology.addama.services.jobsdao.dao.jdbct;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.systemsbiology.addama.services.jobsdao.pojo.Job;

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
        ps.setString(++position, job.getJobUri());
        ps.setString(++position, job.getUserUri());
        ps.setString(++position, job.getLabel());
        ps.setString(++position, job.getJobStatus().name());
        ps.setString(++position, job.getErrorMessage());

        ps.setTimestamp(++position, new Timestamp(job.getCreatedAt().getTime()));
        ps.setTimestamp(++position, new Timestamp(job.getModifiedAt().getTime()));
    }
}
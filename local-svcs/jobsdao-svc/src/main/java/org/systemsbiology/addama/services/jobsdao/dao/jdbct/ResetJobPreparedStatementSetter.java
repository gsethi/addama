package org.systemsbiology.addama.services.jobsdao.dao.jdbct;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.systemsbiology.addama.services.jobsdao.pojo.JobStatus;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author hrovira
 */
public class ResetJobPreparedStatementSetter implements PreparedStatementSetter {
    private final JobStatus currentStatus;
    private final JobStatus newStatus;

    public ResetJobPreparedStatementSetter(JobStatus currentStatus, JobStatus newstatus) {
        this.currentStatus = currentStatus;
        this.newStatus = newstatus;
    }

    public void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, newStatus.name());
        ps.setString(2, currentStatus.name());
    }

}
package org.systemsbiology.addama.services.jobsdao.dao.jdbct;

import static org.apache.commons.lang.StringUtils.isEmpty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.services.jobsdao.pojo.Job;
import org.systemsbiology.addama.services.jobsdao.pojo.JobStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author hrovira
 */
public class JobResultSetExtractor implements ResultSetExtractor {
    private final ArrayList<Job> jobs = new ArrayList<Job>();

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
            String jobUri = rs.getString("URI");
            String userUri = rs.getString("USER");

            String label = rs.getString("LABEL");
            String status = rs.getString("JOB_STATUS");
            String errorMsg = rs.getString("ERROR_MSG");
            Integer returnCode = rs.getInt("RETURN_CODE");
            Date createdAt = rs.getTimestamp("CREATED_AT");
            Date modifiedAt = rs.getTimestamp("MODIFIED_AT");

            Job job = new Job(jobUri, userUri);
            job.setCreatedAt(createdAt);
            job.setModifiedAt(modifiedAt);
            job.setLabel(label);
            job.setErrorMessage(errorMsg);
            job.setReturnCode(returnCode);

            if (!isEmpty(status)) {
                job.setJobStatus(JobStatus.valueOf(status));
            }
            jobs.add(job);
        }
        return null;
    }

    public Job[] getJobs() {
        return jobs.toArray(new Job[jobs.size()]);
    }
}
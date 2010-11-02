package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.services.execution.dao.Job;
import org.systemsbiology.addama.services.execution.dao.JobStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class JobResultSetExtractor implements ResultSetExtractor {
    private static final Logger log = Logger.getLogger(JobResultSetExtractor.class.getName());

    private final ArrayList<Job> jobs = new ArrayList<Job>();

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
            String jobUri = rs.getString("URI");
            String scriptUri = rs.getString("SCRIPT");
            String userUri = rs.getString("USER");
            String jobDir = rs.getString("JOB_DIR");
            JSONObject inputsJson = getInputs(rs);

            String label = rs.getString("LABEL");
            String status = rs.getString("JOB_STATUS");
            String execDir = rs.getString("EXEC_DIR");
            String errorMsg = rs.getString("ERROR_MSG");
            Date createdAt = rs.getTimestamp("CREATED_AT");
            Date modifiedAt = rs.getTimestamp("MODIFIED_AT");

            Job job = new Job(jobUri, scriptUri, userUri, jobDir, inputsJson);
            job.setCreatedAt(createdAt);
            job.setModifiedAt(modifiedAt);

            if (!StringUtils.isEmpty(label)) job.setLabel(label);
            if (!StringUtils.isEmpty(status)) job.setJobStatus(JobStatus.valueOf(status));
            if (!StringUtils.isEmpty(execDir)) job.setExecutionDirectory(execDir);
            if (!StringUtils.isEmpty(errorMsg)) job.setErrorMessage(errorMsg);
            jobs.add(job);
        }
        return null;
    }

    public Job[] getJobs() {
        return jobs.toArray(new Job[jobs.size()]);
    }

    private JSONObject getInputs(ResultSet rs) {
        try {
            String inputs = rs.getString("INPUTS");
            if (!StringUtils.isEmpty(inputs)) {
                return new JSONObject(inputs);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return null;
    }
}

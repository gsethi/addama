package org.systemsbiology.addama.services.execution.dao.impls.jdbc;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.systemsbiology.addama.services.execution.jobs.Job;
import org.systemsbiology.addama.services.execution.jobs.JobStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class JobResultSetExtractor implements ResultSetExtractor {
    private final ArrayList<Job> jobs = new ArrayList<Job>();

    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
            String jobUri = rs.getString("URI");
            String scriptUri = rs.getString("SCRIPT");
            String userUri = rs.getString("USER");
            String jobDir = rs.getString("JOB_DIR");

            String label = rs.getString("LABEL");
            String status = rs.getString("JOB_STATUS");
            String execDir = rs.getString("EXEC_DIR");
            String errorMsg = rs.getString("ERROR_MSG");
            Integer returnCode = rs.getInt("RETURN_CODE");
            Date createdAt = rs.getTimestamp("CREATED_AT");
            Date modifiedAt = rs.getTimestamp("MODIFIED_AT");

            String scriptPath = rs.getString("SCRIPT_PATH");
            String scriptArgs = rs.getString("SCRIPT_ARGS");
            String channelUri = rs.getString("CHANNEL_URI");

            Job job = new Job(jobUri, scriptUri, userUri, jobDir, scriptPath);
            job.setCreatedAt(createdAt);
            job.setModifiedAt(modifiedAt);
            job.setScriptArgs(scriptArgs);
            job.setChannelUri(channelUri);
            job.setLabel(label);
            job.setExecutionDirectory(execDir);
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

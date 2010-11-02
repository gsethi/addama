package org.systemsbiology.addama.services.execution.dao.impls;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.services.execution.dao.Job;
import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.CreateJobPreparedStatementCallback;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.JobResultSetExtractor;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.UpdateJobPreparedStatementCallback;

import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class JdbcTemplateJobsDao implements JobsDao {
    private static final Logger log = Logger.getLogger(JdbcTemplateJobsDao.class.getName());

    private final JdbcTemplate jdbcTemplate;

    public JdbcTemplateJobsDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * JobsDao
     */

    public Job retrieve(String jobUri) {
        log.info(jobUri);

        Job[] jobs = retrieve("SELECT * FROM JOBS WHERE URI = ?", jobUri);
        if (jobs.length > 0) {
            return jobs[0];
        }
        return null;
    }

    public void create(Job job) {
        log.info(job.getJobUri());

        String sql = "INSERT INTO JOBS (URI, SCRIPT, USER, JOB_DIR, INPUTS, LABEL, JOB_STATUS, " +
                "EXEC_DIR, ERROR_MSG, CREATED_AT, MODIFIED_AT)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?);";
        jdbcTemplate.execute(sql, new CreateJobPreparedStatementCallback(job));
    }

    public void modify(Job job) {
        log.info(job.getJobUri());

        String sql = "UPDATE JOBS SET JOB_STATUS = ?, ERROR_MSG = ?, MODIFIED_AT = ? WHERE URI = ?;";
        jdbcTemplate.execute(sql, new UpdateJobPreparedStatementCallback(job));
    }

    public Job[] retrieveAllForScript(String scriptUri) {
        return retrieve("SELECT * FROM JOBS WHERE SCRIPT = ?", scriptUri);
    }

    public Job[] retrieveAllForScript(String scriptUri, String userUri) {
        return retrieve("SELECT * FROM JOBS WHERE SCRIPT = ? AND USER = ?", scriptUri, userUri);
    }

    public Job[] retrieveAllForUser(String userUri) {
        return retrieve("SELECT * FROM JOBS WHERE USER = ?", userUri);
    }

    /*
    * Private Methods
    */

    private Job[] retrieve(String sql, String... params) {
        log.info(sql + ":" + StringUtils.join(params, ","));

        JobResultSetExtractor extractor = new JobResultSetExtractor();
        jdbcTemplate.query(sql, params, extractor);
        return extractor.getJobs();
    }

}

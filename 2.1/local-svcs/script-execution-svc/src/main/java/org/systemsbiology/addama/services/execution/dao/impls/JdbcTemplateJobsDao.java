package org.systemsbiology.addama.services.execution.dao.impls;

import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.CreateJobPreparedStatementSetter;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.JobResultSetExtractor;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.ResetJobPreparedStatementSetter;
import org.systemsbiology.addama.services.execution.dao.impls.jdbc.UpdateJobPreparedStatementSetter;
import org.systemsbiology.addama.services.execution.jobs.Job;
import org.systemsbiology.addama.services.execution.jobs.JobStatus;

import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.join;
import static org.systemsbiology.addama.services.execution.jobs.JobStatus.pending;

/**
 * @author hrovira
 */
public class JdbcTemplateJobsDao implements JobsDao {
    private static final Logger log = Logger.getLogger(JdbcTemplateJobsDao.class.getName());

    private final JdbcTemplate jdbcTemplate;

    public JdbcTemplateJobsDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.initializeDb();
    }

    /*
     * JobsDao
     */

    public Job retrieve(String jobUri) {
        log.fine(jobUri);

        Job[] jobs = retrieve("SELECT * FROM JOBS WHERE URI = ?", jobUri);
        if (jobs.length > 0) {
            return jobs[0];
        }
        return null;
    }

    public void create(Job job) {
        log.fine(job.getJobUri());

        String sql = "INSERT INTO JOBS (URI, SCRIPT, USER, JOB_DIR, LABEL, JOB_STATUS, " +
                "EXEC_DIR, ERROR_MSG, SCRIPT_PATH, SCRIPT_ARGS, CHANNEL_URI, CREATED_AT, MODIFIED_AT)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?);";
        jdbcTemplate.update(sql, new CreateJobPreparedStatementSetter(job));
    }

    public void update(Job job) {
        log.fine(job.getJobUri());

        String sql = "UPDATE JOBS SET JOB_STATUS = ?, ERROR_MSG = ?, MODIFIED_AT = ?, RETURN_CODE = ? WHERE URI = ?;";
        jdbcTemplate.update(sql, new UpdateJobPreparedStatementSetter(job));
    }

    public void delete(Job job) {
        log.fine(job.getJobUri());

        jdbcTemplate.update("DELETE FROM JOBS WHERE URI = ?;", new Object[]{job.getJobUri()});
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

    public Job[] retrievePendingJobs() {
        return retrieve("SELECT * FROM JOBS WHERE JOB_STATUS = ?", pending.name());
    }

    public void resetJobs(JobStatus current, JobStatus newstatus) {
        log.fine(current.name() + "->" + newstatus);

        String sql = "UPDATE JOBS SET JOB_STATUS = ? WHERE JOB_STATUS = ?;";
        jdbcTemplate.update(sql, new ResetJobPreparedStatementSetter(current, newstatus));
    }

    /*
    * Private Methods
    */

    private Job[] retrieve(String sql, String... params) {
        log.fine(sql + ":" + join(params, ","));

        JobResultSetExtractor extractor = new JobResultSetExtractor();
        jdbcTemplate.query(sql, params, extractor);
        return extractor.getJobs();
    }

    private void initializeDb() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS JOBS (");
        sql.append("  URI TEXT,");
        sql.append("  SCRIPT TEXT,");
        sql.append("  USER TEXT,");
        sql.append("  JOB_DIR TEXT,");
        sql.append("  LABEL TEXT,");
        sql.append("  JOB_STATUS TEXT,");
        sql.append("  EXEC_DIR TEXT,");
        sql.append("  CREATED_AT DATETIME,");
        sql.append("  MODIFIED_AT DATETIME,");
        sql.append("  ERROR_MSG TEXT,");
        sql.append("  SCRIPT_PATH TEXT,");
        sql.append("  SCRIPT_ARGS TEXT,");
        sql.append("  CHANNEL_URI TEXT,");
        sql.append("  RETURN_CODE INT");
        sql.append(");");

        log.fine("executing: " + sql.toString());
        this.jdbcTemplate.execute(sql.toString());
    }
}

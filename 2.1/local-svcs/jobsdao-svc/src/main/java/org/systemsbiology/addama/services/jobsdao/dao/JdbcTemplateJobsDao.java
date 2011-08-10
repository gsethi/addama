package org.systemsbiology.addama.services.jobsdao.dao;

import static org.apache.commons.lang.StringUtils.join;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.services.jobsdao.dao.jdbct.CreateJobPreparedStatementSetter;
import org.systemsbiology.addama.services.jobsdao.dao.jdbct.JobResultSetExtractor;
import org.systemsbiology.addama.services.jobsdao.dao.jdbct.UpdateJobPreparedStatementSetter;
import org.systemsbiology.addama.services.jobsdao.pojo.Job;
import static org.systemsbiology.addama.services.jobsdao.pojo.JobStatus.pending;

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
        log.fine(jobUri);

        Job[] jobs = retrieve("SELECT * FROM JOBS WHERE URI = ?", jobUri);
        if (jobs.length > 0) {
            return jobs[0];
        }
        return null;
    }

    public void create(Job job) {
        log.fine(job.getJobUri());

        String sql = "INSERT INTO JOBS (URI, USER, LABEL, JOB_STATUS, ERROR_MSG, CREATED_AT, MODIFIED_AT)" +
                " VALUES (?,?,?,?,?,?,?);";
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

    public Job[] retrieveAll() {
        return retrieve("SELECT * FROM JOBS", new String[0]);
    }

    public Job[] retrievePendingJobs() {
        return retrieve("SELECT * FROM JOBS WHERE JOB_STATUS = ?", pending.name());
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
}
package org.systemsbiology.addama.services.execution.dao;

import java.util.Date;

/**
 * @author hrovira
 */
public class JobUpdater {
    private final Job job;
    private final JobsDao jobsDao;

    public JobUpdater(Job job, JobsDao jobsDao) {
        this.job = job;
        this.jobsDao = jobsDao;
    }

    public void scheduled() {
        persistJob(JobStatus.scheduled);
    }

    public void running() {
        persistJob(JobStatus.running);
    }

    public void completed() {
        persistJob(JobStatus.completed);
    }

    public void onError(Exception e) {
        job.setErrorMessage(e.getMessage());
        persistJob(JobStatus.errored);
    }

    /*
    * Private Methods
    */

    private void persistJob(JobStatus jobStatus) {
        job.setJobStatus(jobStatus);
        job.setModifiedAt(new Date());
        jobsDao.modify(job);
    }
}

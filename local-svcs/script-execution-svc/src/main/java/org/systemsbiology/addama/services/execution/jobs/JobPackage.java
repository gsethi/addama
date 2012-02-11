package org.systemsbiology.addama.services.execution.jobs;

import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.notification.JobNotifier;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.logging.Logger;

import static org.systemsbiology.addama.services.execution.jobs.JobStatus.*;

/**
 * @author hrovira
 */
public class JobPackage {
    private static final Logger log = Logger.getLogger(JobPackage.class.getName());

    private final String jobId;
    private final JobsDao jobsDao;
    private final ReturnCodes returnCodes;
    private final JobNotifier[] jobNotifiers;

    public JobPackage(Job job, JobsDao jobsDao, ReturnCodes returnCodes, JobNotifier... jobNotifiers) {
        this.jobId = job.getJobId();
        this.jobsDao = jobsDao;
        this.returnCodes = returnCodes;
        this.jobNotifiers = jobNotifiers;
    }

    public String getJobId() {
        return jobId;
    }

    public Job retrieve() {
        return jobsDao.retrieve(jobId);
    }

    public void completed(int result) {
        Job job = retrieve();
        if (job.getJobStatus().equals(stopping)) {
            persistJob(job, stopped);
            return;
        }

        job.setReturnCode(result);

        if (returnCodes != null) {
            Integer success = returnCodes.getSuccessCode();
            if (result == success) {
                persistJob(job, completed);
            } else {
                job.setErrorMessage(returnCodes.getReason(result));
                persistJob(job, errored);
            }
        } else {
            persistJob(job, completed);
        }
    }

    public void onError(Exception e) {
        Job job = retrieve();
        job.setErrorMessage(e.getMessage());
        persistJob(job, errored);
    }

    public void changeStatus(JobStatus status) {
        persistJob(retrieve(), status);
    }

    /*
    * Private Methods
    */

    private void persistJob(Job job, JobStatus jobStatus) {
        log.fine(job.getJobId() + ":" + jobStatus);

        job.setJobStatus(jobStatus);
        job.setModifiedAt(new Date());
        jobsDao.update(job);

        serialize(job);

        if (jobNotifiers != null) {
            for (JobNotifier jobNotifier : jobNotifiers) {
                jobNotifier.notify(job);
            }
        }
    }

    private void serialize(Job job) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(job.getJobDirectory(), "job.json"));
            String jsonOut = job.getJsonDetail().toString(4) + "\n";
            fos.write(jsonOut.getBytes());
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }
}

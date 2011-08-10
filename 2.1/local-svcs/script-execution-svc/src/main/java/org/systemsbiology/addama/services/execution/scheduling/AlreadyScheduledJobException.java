package org.systemsbiology.addama.services.execution.scheduling;

import org.systemsbiology.addama.services.execution.jobs.JobPackage;

/**
 * @author hrovira
 */
public class AlreadyScheduledJobException extends Exception {
    public AlreadyScheduledJobException(JobPackage jobPackage) {
        super("Job has already been scheduled:" + jobPackage.getJobUri());
    }
}

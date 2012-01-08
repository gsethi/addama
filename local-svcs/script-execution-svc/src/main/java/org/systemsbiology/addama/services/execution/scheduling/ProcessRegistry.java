package org.systemsbiology.addama.services.execution.scheduling;

import org.systemsbiology.addama.services.execution.jobs.JobPackage;

import java.util.HashMap;

/**
 * @author hrovira
 */
public class ProcessRegistry {
    private final HashMap<String, Process> processesByJobId = new HashMap<String, Process>();

    public void start(JobPackage jobPackage, Process p) throws AlreadyScheduledJobException {
        String jobId = jobPackage.getJobId();
        Process existing = processesByJobId.get(jobId);
        if (existing != null) {
            // handle race conditions
            throw new AlreadyScheduledJobException(jobPackage);
        }

        processesByJobId.put(jobId, p);
    }

    public void end(JobPackage jobPackage) {
        String jobId = jobPackage.getJobId();
        if (processesByJobId.containsKey(jobId)) {
            Process p = processesByJobId.remove(jobId);
            if (p != null) {
                p.destroy();
            }
        }
    }
}

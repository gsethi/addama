package org.systemsbiology.addama.services.execution.scheduling;

import org.systemsbiology.addama.services.execution.jobs.JobPackage;

import java.util.HashMap;

/**
 * @author hrovira
 */
public class ProcessRegistry {
    private final HashMap<String, Process> processesByJobUri = new HashMap<String, Process>();

    public void start(JobPackage jobPackage, Process p) throws AlreadyScheduledJobException {
        String jobUri = jobPackage.getJobUri();
        Process existing = processesByJobUri.get(jobUri);
        if (existing != null) {
            // handle race conditions
            throw new AlreadyScheduledJobException(jobPackage);
        }

        processesByJobUri.put(jobUri, p);
    }

    public void end(JobPackage jobPackage) {
        String jobUri = jobPackage.getJobUri();
        if (processesByJobUri.containsKey(jobUri)) {
            Process p = processesByJobUri.remove(jobUri);
            if (p != null) {
                p.destroy();
            }
        }
    }
}

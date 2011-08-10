package org.systemsbiology.addama.services.execution.notification;

import org.systemsbiology.addama.services.execution.jobs.Job;

/**
 * @author hrovira
 */
public interface JobNotifier {
    public void notify(Job job);
}

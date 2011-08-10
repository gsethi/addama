package org.systemsbiology.addama.services.execution.scheduling;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;
import org.systemsbiology.addama.services.execution.jobs.JobPackage;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author hrovira
 */
public class JobQueuesJsonConfigHandler extends GenericMapJsonConfigHandler<Queue<JobPackage>> {

    public JobQueuesJsonConfigHandler(Map<String, Queue<JobPackage>> jobQueuesByUri) {
        super(jobQueuesByUri);
    }

    public Queue<JobPackage> getSpecific(JSONObject item) throws Exception {
        return new ConcurrentLinkedQueue<JobPackage>();
    }
}

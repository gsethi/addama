package org.systemsbiology.addama.services.execution.scheduling;

import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;
import org.systemsbiology.addama.services.execution.jobs.JobPackage;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author hrovira
 */
public class JobQueuesMappingsHandler extends MappingPropertyByIdContainer<Queue<JobPackage>> implements MappingsHandler {

    public JobQueuesMappingsHandler(Map<String, Queue<JobPackage>> jobQueuesByUri) {
        super(jobQueuesByUri);
    }

    public void handle(Mapping mapping) throws Exception {
        addValue(mapping, new ConcurrentLinkedQueue<JobPackage>());
    }
}

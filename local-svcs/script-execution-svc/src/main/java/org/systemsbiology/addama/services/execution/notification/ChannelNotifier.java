package org.systemsbiology.addama.services.execution.notification;

import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.NoOpResponseCallback;
import org.systemsbiology.addama.services.execution.jobs.Job;

import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class ChannelNotifier implements JobNotifier {
    private static final Logger log = Logger.getLogger(ChannelNotifier.class.getName());

    private HttpClientTemplate httpClientTemplate;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void notify(Job job) {
        if (isEmpty(job.getChannelUri())) {
            log.warning("no channel configured for publishing");
            return;
        }

        try {
            new Thread(new Async(job));
        } catch (Exception e) {
            log.warning("error sending update for job [" + job.getJobId() + "]:" + e.getMessage());
        }
    }

    private class Async implements Runnable {
        private final String jobId;
        private final String channelUri;
        private final JSONObject event;

        private Async(Job job) throws Exception {
            // TODO : Remove channel URI, use owner
            this.jobId = job.getJobId();
            this.channelUri = job.getChannelUri();
            this.event = job.getJsonSummary();
        }

        public void run() {
            try {
                JSONObject json = new JSONObject();
                json.put("job", event);

                PostMethod post = new PostMethod(channelUri);
                post.addParameter("event", json.toString());

                httpClientTemplate.executeMethod(post, new NoOpResponseCallback());
            } catch (Exception e) {
                log.warning("error sending update for job [" + jobId + "]:" + e.getMessage());
            }
        }
    }
}

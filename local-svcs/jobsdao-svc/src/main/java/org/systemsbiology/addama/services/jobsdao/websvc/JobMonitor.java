package org.systemsbiology.addama.services.jobsdao.websvc;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientResponseException;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.ResponseCallback;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author: hrovira
 */
public class JobMonitor {
    private static final Logger log = Logger.getLogger(JobMonitor.class.getName());

    public static void main(String[] args) throws Exception {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(new URI("http://localhost:8083"));

        HttpClient httpClient = new HttpClient();
        httpClient.setHostConfiguration(hostConfig);

        HttpClientTemplate template = new HttpClientTemplate(httpClient);
        template.afterPropertiesSet();

        log.info("ready");

        template.executeMethod(new GetMethod("/jobs/"), new StdoutResponseCallback());

        PostMethod post = new PostMethod("/jobs/");
//        post.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
//        post.setRequestHeader("Accept", "text/plain");

        JSONObject job = new JSONObject();
        job.put("Count", 1);
        job.append("Args", "echo");
        job.append("Args", "'Hello Golem'");

        JSONArray jobs = new JSONArray();
        jobs.put(0, job);

        post.setParameter("data", jobs.toString());
        post.setParameter("command", "run");

        template.executeMethod(post, new StdoutResponseCallback());

        template.executeMethod(new GetMethod("/jobs/"), new StdoutResponseCallback());
    }

    private static class StdoutResponseCallback implements ResponseCallback {
        public Object onResponse(int statusCode, HttpMethod method) throws HttpClientResponseException {
            try {
                log.info("statusCode=" + statusCode);
                log.info("response=" + method.getResponseBodyAsString());
            } catch (IOException e) {
                throw new HttpClientResponseException(statusCode, method, e);
            }
            return null;
        }
    }
}


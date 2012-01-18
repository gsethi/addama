package org.systemsbiology.addama.services.execution.util;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.Cookie;

import static org.junit.Assert.*;
import static org.systemsbiology.addama.services.execution.util.HttpJob.getScriptExecution;
import static org.systemsbiology.addama.services.execution.util.HttpJob.getUserUri;

/**
 * @author hrovira
 */
public class HttpJobTest {
    private static final String EXEC = "executable";
    private static final String SCRIPT = "/usr/bin/sample.sh";
    private static final String ARGS = "arg1 arg2 arg3";

    @Test
    public void getScriptExecution_normal() {
        Job job = new Job(null, null, null, null, null, SCRIPT);
        job.setScriptArgs(ARGS);

        String[] args = getScriptExecution(job);

        assertArrays(args, SCRIPT, ARGS);
    }

    @Test
    public void getScriptExecution_normal_withexec() {
        Job job = new Job(null, null, null, null, null, EXEC + " " + SCRIPT);
        job.setScriptArgs(ARGS);

        String[] args = getScriptExecution(job);

        assertArrays(args, EXEC, SCRIPT, ARGS);
    }

    @Test
    public void getScriptExecution_normal_noargs() {
        Job job = new Job(null, null, null, null, null, SCRIPT);

        String[] args = getScriptExecution(job);

        assertArrays(args, SCRIPT);
    }

    @Test
    public void getScriptExecution_normal_withexec_noargs() {
        Job job = new Job(null, null, null, null, null, EXEC + " " + SCRIPT);

        String[] args = getScriptExecution(job);

        assertArrays(args, EXEC, SCRIPT);
    }

    @Test
    public void getScriptExecution_noscript() {
        Job job = new Job(null, null, null, null, null, null);

        String[] args = getScriptExecution(job);

        assertArrays(args);
    }

    @Test
    public void cookie_valid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("x-addama-registry-user", "test1"));

        String userUri = getUserUri(request);
        assertNotNull(userUri);
        assertEquals("test1", userUri);
    }

    @Test
    public void cookie_null() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String userUri = getUserUri(request);
        assertNull(userUri);
    }

    private void assertArrays(String[] args, String... expected) {
        assertNotNull(args);
        assertEquals(expected.length, args.length);

        for (int i = 0; i < args.length; i++) {
            assertEquals(expected[i], args[i]);
        }
    }
}

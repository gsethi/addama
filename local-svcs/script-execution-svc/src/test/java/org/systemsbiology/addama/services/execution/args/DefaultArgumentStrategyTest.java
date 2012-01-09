package org.systemsbiology.addama.services.execution.args;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.systemsbiology.addama.services.execution.jobs.Job;

import static org.apache.commons.lang.StringUtils.contains;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class DefaultArgumentStrategyTest {
    private final DefaultArgumentStrategy strategy = new DefaultArgumentStrategy();

    @Test
    public void simple() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("test", "blah");

        Job j = new Job(null, null, null, null, null, null);

        strategy.handle(j, request);

        assertNotNull(j.getScriptArgs());
        assertTrue(contains(j.getScriptArgs(), "test=blah"));
    }

    @Test
    public void noargs() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Job j = new Job(null, null, null, null, null, null);

        strategy.handle(j, request);

        assertTrue(isEmpty(j.getScriptArgs()));
        assertFalse(contains(j.getScriptArgs(), "test=blah"));
    }
}

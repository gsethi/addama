package org.systemsbiology.addama.services.execution.args;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.systemsbiology.addama.services.execution.jobs.Job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author hrovira
 */
public class ParseScriptVarsArgumentStrategyTest {
    private final ParseScriptVarsArgumentStrategy strategy = new ParseScriptVarsArgumentStrategy();

    @Test
    public void simple() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("arg_1", "value_1");
        request.setParameter("arg_2", "value_2");
        request.setParameter("arg_3", "value_3");

        Job j = new Job(null, null, null, null, "/usr/bin/example.sh ${arg_1} ${arg_2} ${arg_3}");

        strategy.handle(j, request);

        assertNotNull(j.getScriptPath());
        assertEquals("/usr/bin/example.sh", j.getScriptPath());
        assertEquals("value_1 value_2 value_3", j.getScriptArgs());
    }

    @Test
    public void noargs() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Job j = new Job(null, null, null, null, "/usr/bin/example.sh ${arg_1} ${arg_2} ${arg_3}");

        strategy.handle(j, request);

        assertEquals("${arg_1} ${arg_2} ${arg_3}", j.getScriptArgs());
    }
}

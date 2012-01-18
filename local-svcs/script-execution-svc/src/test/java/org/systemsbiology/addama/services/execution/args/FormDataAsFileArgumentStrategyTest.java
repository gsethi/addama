package org.systemsbiology.addama.services.execution.args;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.systemsbiology.addama.services.execution.jobs.Job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.junit.Assert.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.pipe;
import static org.systemsbiology.addama.services.execution.args.FormDataAsFileArgumentStrategy.FORM_DATA;
import static org.systemsbiology.addama.services.execution.util.IOJob.mkdirs;

/**
 * @author hrovira
 */
public class FormDataAsFileArgumentStrategyTest {
    private final FormDataAsFileArgumentStrategy strategy = new FormDataAsFileArgumentStrategy();
    private MockHttpServletRequest request;
    private ByteArrayOutputStream outputStream;
    private File formData;
    private Job job;

    @Before
    public void setup() throws Exception {
        request = new MockHttpServletRequest();

        job = new Job(null, null, null, null, null, null);
        job.setExecutionDirectory("target/form-data-as-file-argument-strategy-test");

        mkdirs(new File(job.getExecutionDirectory()));

        formData = new File(job.getExecutionDirectory(), FORM_DATA);

        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void simple() throws Exception {
        request.setContent("test=blah".getBytes());

        strategy.handle(job, request);

        assertNotNull(job.getScriptArgs());
        assertEquals(job.getScriptArgs(), FORM_DATA);

        pipe(new FileInputStream(formData), outputStream);

        String contents = new String(outputStream.toByteArray());
        assertNotNull(contents);
        assertEquals("test=blah", contents);
    }

    @Test
    public void noargs() throws Exception {
        request.setContent("".getBytes());
        strategy.handle(job, request);

        assertNotNull(job.getScriptArgs());
        assertEquals(job.getScriptArgs(), FORM_DATA);

        pipe(new FileInputStream(formData), outputStream);

        assertTrue(isEmpty(new String(outputStream.toByteArray())));
    }


}

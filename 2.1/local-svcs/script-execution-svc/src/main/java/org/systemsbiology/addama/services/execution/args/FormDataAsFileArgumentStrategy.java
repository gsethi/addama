package org.systemsbiology.addama.services.execution.args;

import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.pipe_close;
import static org.systemsbiology.addama.services.execution.util.IOJob.mkdirs;

/**
 * @author hrovira
 */
public class FormDataAsFileArgumentStrategy implements ArgumentStrategy {
    protected static final String FORM_DATA = "form.dat";

    public void handle(Job job, HttpServletRequest request) throws Exception {
        job.setScriptArgs(FORM_DATA);

        mkdirs(new File(job.getExecutionDirectory()));

        FileOutputStream fos = new FileOutputStream(new File(job.getExecutionDirectory(), FORM_DATA));

        InputStream inputStream = request.getInputStream();
        if (inputStream != null) {
            pipe_close(inputStream, fos);
        }
    }
}

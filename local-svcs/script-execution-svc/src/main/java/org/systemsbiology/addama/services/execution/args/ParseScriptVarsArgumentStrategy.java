package org.systemsbiology.addama.services.execution.args;

import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class ParseScriptVarsArgumentStrategy implements ArgumentStrategy {
    public void handle(Job job, HttpServletRequest request) {
        String newscript = substringBefore(job.getScriptPath(), "${");

        String newargs = substringAfter(job.getScriptPath(), newscript);

        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            String varName = "${" + parameterName + "}";
            if (contains(newargs, varName)) {
                newargs = replace(newargs, varName, request.getParameter(parameterName));
            }
        }

        job.setScriptPath(newscript.trim());
        job.setScriptArgs(newargs);
    }
}

package org.systemsbiology.addama.services.execution.args;

import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class DefaultArgumentStrategy implements ArgumentStrategy {

    public void handle(Job job, HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();

        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            if (!equalsIgnoreCase("label", paramName)) {
                for (String paramValue : request.getParameterValues(paramName)) {
                    if (!isEmpty(paramValue)) {
                        builder.append(paramName).append("=").append(paramValue).append("&");
                    }
                }
            }
        }

        job.setScriptArgs(chomp(builder.toString(), "&"));
    }

}

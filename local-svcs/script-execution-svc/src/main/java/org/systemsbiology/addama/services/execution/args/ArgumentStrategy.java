package org.systemsbiology.addama.services.execution.args;

import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.HttpServletRequest;

/**
 * @author hrovira
 */
public interface ArgumentStrategy {

    enum Strategy {
        requestParamsAsArgs, formDataAsFile, parseScriptVars
    }

    void handle(Job job, HttpServletRequest request) throws Exception;

}

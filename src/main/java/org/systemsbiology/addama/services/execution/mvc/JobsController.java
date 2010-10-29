/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.services.execution.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.services.execution.dao.Job;
import org.systemsbiology.addama.services.execution.io.Streamer;
import org.systemsbiology.addama.services.execution.jobs.Executer;
import org.systemsbiology.addama.services.execution.jobs.ResultsExecutionCallback;
import org.systemsbiology.addama.services.execution.util.Mailer;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class JobsController extends BaseController {
    private static final Logger log = Logger.getLogger(JobsController.class.getName());

    @RequestMapping(value = "/**/jobs", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView executeJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/jobs");
        if (!workDirsByUri.containsKey(scriptUri) && !scriptsByUri.containsKey(scriptUri)) {
            throw new ResourceNotFoundException(scriptUri);
        }

        String jobId = UUID.randomUUID().toString();
        String jobUri = scriptUri + "/jobs/" + jobId;
        String jobDir = workDirsByUri.get(scriptUri) + "/jobs/" + jobId;
        String script = scriptsByUri.get(scriptUri) + " \"" + getQueryString(request) + "\"";
        String label = request.getParameter("label");

        Job job = new Job(jobUri, jobDir, request.getHeader("x-addama-registry-user"));
        job.setLabel(label);
        job.setExecutionDirectory(getJobExecutionDirectory(scriptUri));

        // TODO: Schedule Job
        Mailer mailer = getMailer(job);
        executeRobot(script, job, mailer);

        jobsDao.create(job);

        return new ModelAndView(new JsonView()).addObject("json", job.getJsonSummary());
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJobs(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/jobs");

        JSONObject json = new JSONObject();
        Job[] jobs = jobsDao.retrieveAllForScript(scriptUri, request.getHeader("x-addama-registry-user"));
        for (Job job : jobs) {
            json.append("items", job.getJsonSummary());
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String jobUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        Job job = jobsDao.retrieve(jobUri);

        return new ModelAndView(new JsonView()).addObject("json", job.getJsonDetail());
    }

    /*
     * Private Methods
     */

    private void executeRobot(String script, Job job, Mailer mailer) throws Exception {
        log.fine(script);

        File execAtDir = new File(job.getExecuteAtPath());
        execAtDir.mkdirs();

        File outputDir = new File(job.getOutputDirectoryPath());
        outputDir.mkdirs();

        Process p = Runtime.getRuntime().exec(script, new String[0], execAtDir);

        String joblog = job.getLogPath();
        OutputStream logStream = new FileOutputStream(joblog);
        Streamer stdoutStreamer = new Streamer(p.getInputStream(), logStream, true);
        Streamer errorStreamer = new Streamer(p.getErrorStream(), logStream, true);

        if (mailer != null) {
            mailer.setJobLog(joblog);
        }

        Runnable executer = new Executer(p, new ResultsExecutionCallback(job, stdoutStreamer, errorStreamer, logStream), mailer);

        new Thread(stdoutStreamer).start();
        new Thread(errorStreamer).start();
        new Thread(executer).start();
    }

    private String getQueryString(HttpServletRequest request) {
        if (request.getMethod().equalsIgnoreCase("get")) {
            return request.getQueryString();
        }

        StringBuilder builder = new StringBuilder();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            for (String paramValue : paramValues) {
                builder.append(paramName).append("=").append(paramValue).append("&");
            }
        }
        log.info("getQueryString(" + request.getRequestURI() + "):[" + builder.toString() + "]");
        String queryString = builder.toString();
        if (queryString.endsWith("&")) {
            return StringUtils.substringBeforeLast(queryString, "&");
        }
        return queryString;
    }
}

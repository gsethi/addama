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
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.ResourceNotFoundView;
import org.systemsbiology.addama.services.execution.io.Streamer;
import org.systemsbiology.addama.services.execution.jobs.Executer;
import org.systemsbiology.addama.services.execution.jobs.Results;
import org.systemsbiology.addama.services.execution.jobs.ResultsExecutionCallback;
import org.systemsbiology.addama.services.execution.util.Mailer;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class JobsController extends BaseController {
    private static final Logger log = Logger.getLogger(JobsController.class.getName());

    private final HashMap<String, Results> resultsByUri = new HashMap<String, Results>();


    @RequestMapping(value = "/**/execution", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView execution(HttpServletRequest request) throws Exception {
        log.info("execution(" + request.getRequestURI() + ")");

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/execution");
        if (!workDirsByUri.containsKey(scriptUri) && !scriptsByUri.containsKey(scriptUri)) {
            throw new ResourceNotFoundException(scriptUri);
        }

        String jobId = UUID.randomUUID().toString();
        String jobUri = scriptUri + "/jobs/" + jobId;
        String jobDir = workDirsByUri.get(scriptUri) + "/jobs/" + jobId;
        String script = scriptsByUri.get(scriptUri) + " \"" + getQueryString(request) + "\"";
        String jobExecutionDirectory = getJobExecutionDirectory(scriptUri);

        Results r = executeRobot(script, jobDir, null, jobExecutionDirectory);

        resultsByUri.put(jobUri, r);
        while (!r.isSuccessful() && !r.hasErrors()) {
            try {
                // wait for results, should be quick
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.info("execution(" + request.getRequestURI() + "):" + e);
                break;
            }
        }

        JSONObject json = getResultsJson(jobUri, r, scriptUri, workDirsByUri.get(scriptUri));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView executeJob(HttpServletRequest request) throws Exception {
        log.info("executeJob(" + request.getRequestURI() + ")");

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/jobs");
        if (!workDirsByUri.containsKey(scriptUri) && !scriptsByUri.containsKey(scriptUri)) {
            throw new ResourceNotFoundException(scriptUri);
        }

        String jobId = UUID.randomUUID().toString();
        String jobUri = scriptUri + "/jobs/" + jobId;
        String jobDir = workDirsByUri.get(scriptUri) + "/jobs/" + jobId;
        String script = scriptsByUri.get(scriptUri) + " \"" + getQueryString(request) + "\"";
        String label = request.getParameter("label");
        String jobExecutionDirectory = getJobExecutionDirectory(scriptUri);

        String user = getUserEmail(request);
        Mailer mailer = getMailer(scriptUri, label, user);
        Results r = executeRobot(script, jobDir, mailer, jobExecutionDirectory);
        r.setLabel(label);

        resultsByUri.put(jobUri, r);

        JSONObject json = new JSONObject();
        json.put("uri", jobUri);
        json.put("label", label);

        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJobs(HttpServletRequest request) throws Exception {
        log.info("getJobs(" + request.getRequestURI() + ")");
        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/jobs");

        JSONObject json = new JSONObject();

        for (Map.Entry<String, Results> entry : resultsByUri.entrySet()) {
            String jobUri = entry.getKey();
            if (jobUri.startsWith(scriptUri)) {
                JSONObject jobjson = new JSONObject();
                jobjson.put("uri", jobUri);

                Results r = entry.getValue();
                if (!StringUtils.isEmpty(r.getLabel())) {
                    jobjson.put("label", r.getLabel());
                }
                json.append("items", jobjson);
            }
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJob(HttpServletRequest request) throws Exception {
        String jobUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());

        Results r = resultsByUri.get(jobUri);
        if (r == null) {
            throw new ResourceNotFoundException(jobUri);
        }

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/jobs");
        if (!workDirsByUri.containsKey(scriptUri)) {
            return new ModelAndView(new ResourceNotFoundView());
        }

        JSONObject json = getResultsJson(jobUri, r, scriptUri, workDirsByUri.get(scriptUri));
        return new ModelAndView(new JsonView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private JSONObject getResultsJson(String jobUri, Results r, String scriptUri, String baseWorkDir) throws JSONException {
        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, r.getOutputDirectory());

        JSONObject json = new JSONObject();
        json.put("uri", jobUri);
        json.put("log", jobUri + "/log");
        if (!StringUtils.isEmpty(r.getLabel())) {
            json.put("label", r.getLabel());
        }
        if (r.isSuccessful()) {
            json.put("status", "completed");
        } else {
            json.put("status", "running");
        }
        if (r.hasErrors()) {
            json.put("status", "error");
            json.put("message", r.getErrorMessage());
        }

        for (File output : outputs) {
            JSONObject outputJson = new JSONObject();
            outputJson.put("uri", scriptUri + StringUtils.substringAfterLast(output.getPath(), baseWorkDir));
            outputJson.put("name", output.getName());
            json.append("outputs", outputJson);
        }

        return json;
    }

    private Results executeRobot(String script, String jobDir, Mailer mailer, String jobExecutionDirectory) throws Exception {
        log.fine("executeRobot(" + script + "," + jobDir + ")");

        String jobExecAt = jobDir;
        if (!StringUtils.isEmpty(jobExecutionDirectory)) {
            jobExecAt = jobDir + "/" + jobExecutionDirectory;
        }

        File execAtDir = new File(jobExecAt);
        execAtDir.mkdirs();
        Process p = Runtime.getRuntime().exec(script, new String[0], execAtDir);

        File outputDir = new File(jobDir + "/outputs");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        Results r = new Results(outputDir);

        String joblog = jobDir + "/job.log";
        OutputStream logStream = new FileOutputStream(joblog);
        Streamer stdoutStreamer = new Streamer(p.getInputStream(), logStream, true);
        Streamer errorStreamer = new Streamer(p.getErrorStream(), logStream, true);

        if (mailer != null) {
            mailer.setJobLog(joblog);
        }
        Runnable executer = new Executer(p, new ResultsExecutionCallback(r, stdoutStreamer, errorStreamer, logStream), mailer);

        new Thread(stdoutStreamer).start();
        new Thread(errorStreamer).start();
        new Thread(executer).start();

        return r;
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

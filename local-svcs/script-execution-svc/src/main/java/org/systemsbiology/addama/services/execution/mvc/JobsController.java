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

import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.commons.web.views.ResourceStateConflictView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.impls.StringMapJsonConfigHandler;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategy;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategyJsonConfigHandler;
import org.systemsbiology.addama.services.execution.dao.JobsDaoAware;
import org.systemsbiology.addama.services.execution.jobs.Job;
import org.systemsbiology.addama.services.execution.jobs.JobPackage;
import org.systemsbiology.addama.services.execution.jobs.ReturnCodes;
import org.systemsbiology.addama.services.execution.jobs.ReturnCodesConfigHandler;
import org.systemsbiology.addama.services.execution.notification.ChannelNotifier;
import org.systemsbiology.addama.services.execution.notification.EmailBean;
import org.systemsbiology.addama.services.execution.notification.EmailJsonConfigHandler;
import org.systemsbiology.addama.services.execution.notification.EmailNotifier;
import org.systemsbiology.addama.services.execution.scheduling.EnvironmentVariablesJsonConfigHandler;
import org.systemsbiology.addama.services.execution.scheduling.JobQueueHandlingRunnable;
import org.systemsbiology.addama.services.execution.scheduling.JobQueuesJsonConfigHandler;
import org.systemsbiology.addama.services.execution.scheduling.ProcessRegistry;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;
import static java.util.UUID.randomUUID;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.systemsbiology.addama.services.execution.jobs.JobStatus.*;
import static org.systemsbiology.addama.services.execution.util.HttpJob.*;
import static org.systemsbiology.addama.services.execution.util.IOJob.mkdirs;
import static org.systemsbiology.addama.services.execution.util.IOJob.recursiveDelete;

/**
 * @author hrovira
 */
@Controller
public class JobsController extends JobsDaoAware implements InitializingBean {
    private static final Logger log = Logger.getLogger(JobsController.class.getName());

    private final Map<String, ReturnCodes> returnCodesByUri = new HashMap<String, ReturnCodes>();
    private final Map<String, Queue<JobPackage>> jobQueuesByUri = new HashMap<String, Queue<JobPackage>>();
    private final Map<String, EmailBean> emailBeansByUri = new HashMap<String, EmailBean>();
    private final Map<String, String> scriptAdminsByUri = new HashMap<String, String>();
    private final Map<String, ArgumentStrategy> argumentStrategiesByUri = new HashMap<String, ArgumentStrategy>();
    private final Map<String, String> workDirsByUri = new HashMap<String, String>();
    private final Map<String, String> scriptsByUri = new HashMap<String, String>();
    private final Map<String, String> logFilesByUri = new HashMap<String, String>();
    private final Map<String, String> viewersByUri = new HashMap<String, String>();
    private final Map<String, String> numberOfThreadsByUri = new HashMap<String, String>();
    private final Map<String, String> jobExecutionDirectoryByUri = new HashMap<String, String>();
    private final Set<String> environmentVariables = new HashSet<String>();

    private final ProcessRegistry processRegistry = new ProcessRegistry();

    private ChannelNotifier channelNotifier;

    public void setChannelNotifier(ChannelNotifier channelNotifier) {
        this.channelNotifier = channelNotifier;
    }

    public void setJsonConfig(JsonConfig jsonConfig) {
        super.setJsonConfig(jsonConfig);
        jsonConfig.visit(new StringMapJsonConfigHandler(workDirsByUri, "workDir"));
        jsonConfig.visit(new StringMapJsonConfigHandler(scriptsByUri, "script"));
        jsonConfig.visit(new StringMapJsonConfigHandler(logFilesByUri, "logFile"));
        jsonConfig.visit(new StringMapJsonConfigHandler(viewersByUri, "viewer"));
        jsonConfig.visit(new StringMapJsonConfigHandler(jobExecutionDirectoryByUri, "jobExecutionDirectory"));
        jsonConfig.visit(new StringMapJsonConfigHandler(scriptAdminsByUri, "scriptAdmin"));
        jsonConfig.visit(new StringMapJsonConfigHandler(numberOfThreadsByUri, "numberOfThreads"));
        jsonConfig.visit(new ReturnCodesConfigHandler(returnCodesByUri));
        jsonConfig.visit(new EmailJsonConfigHandler(emailBeansByUri));
        jsonConfig.visit(new JobQueuesJsonConfigHandler(jobQueuesByUri));
        jsonConfig.visit(new ArgumentStrategyJsonConfigHandler(argumentStrategiesByUri));
        jsonConfig.visit(new EnvironmentVariablesJsonConfigHandler(environmentVariables));
    }

    public void afterPropertiesSet() throws Exception {
        jobsDao.resetJobs(running, pending);
        jobsDao.resetJobs(scheduled, pending);
        jobsDao.resetJobs(stopping, stopped);

        scheduleJob(jobsDao.retrievePendingJobs());

        String[] envVars = environmentVariables.toArray(new String[environmentVariables.size()]);

        for (String scriptUri : jobQueuesByUri.keySet()) {
            Queue<JobPackage> jobQueue = jobQueuesByUri.get(scriptUri);
            for (int j = 0; j < getNumberOfThreads(scriptUri); j++) {
                new Thread(new JobQueueHandlingRunnable(jobQueue, processRegistry, envVars)).start();
            }
        }
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView executeJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String scriptUri = getScriptUri(request, "/jobs");
        scriptExists(scriptUri);

        String jobId = randomUUID().toString();
        String jobUri = scriptUri + "/jobs/" + jobId;
        String workDir = workDirsByUri.get(scriptUri) + "/jobs/" + jobId;
        String scriptPath = scriptsByUri.get(scriptUri);

        Job job = new Job(jobUri, scriptUri, getUserUri(request), workDir, scriptPath);
        job.setExecutionDirectoryFromConfiguration(jobExecutionDirectoryByUri.get(scriptUri));

        // WARNING:  This logic may need to obtain inputstream from request... DO NOT call request.getParameter before this logic, or it will not work!
        // Major design flaw in Servlet Spec or Tomcat... not sure:  https://issues.apache.org/bugzilla/show_bug.cgi?id=47410
        argumentStrategiesByUri.get(scriptUri).handle(job, request);

        job.setLabel(request.getParameter("label"));
        job.setChannelUri(getChannelUri(request));

        jobsDao.create(job);

        scheduleJob(job);

        return new ModelAndView(new JsonView()).addObject("json", job.getJsonSummary());
    }

    @RequestMapping(value = "/**/tools/jobs", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJobsForUser(HttpServletRequest request) throws Exception {
        log.fine(request.getRequestURI());

        String userUri = getUserUri(request);
        Job[] jobs = jobsDao.retrieveAllForUser(userUri);

        JSONObject json = new JSONObject();
        for (Job job : jobs) {
            json.append("items", job.getJsonSummary());
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJobs(HttpServletRequest request) throws Exception {
        log.fine(request.getRequestURI());

        String scriptUri = getScriptUri(request, "/jobs");
        scriptExists(scriptUri);

        Job[] jobs;
        if (isScriptAdmin(request, scriptUri, scriptAdminsByUri)) {
            jobs = jobsDao.retrieveAllForScript(scriptUri);
        } else {
            jobs = jobsDao.retrieveAllForScript(scriptUri, getUserUri(request));
        }

        JSONObject json = new JSONObject();
        for (Job job : jobs) {
            json.append("items", job.getJsonSummary());
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs/*", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJobById(HttpServletRequest request) throws Exception {
        log.fine(request.getRequestURI());

        Job job = getJob(jobsDao, request, null);
        return new ModelAndView(new JsonView()).addObject("json", job.getJsonDetail());
    }

    @RequestMapping(value = "/**/jobs/*/stop", method = POST)
    @ModelAttribute
    public ModelAndView stopJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        Job job = getJob(jobsDao, request, "/stop");
        if (isScriptOwner(request, job) || isScriptAdmin(request, job.getScriptUri(), scriptAdminsByUri)) {
            switch (job.getJobStatus()) {
                case pending:
                case scheduled:
                case running:
                    // stops queued job
                    JobPackage pkg = new JobPackage(job, jobsDao, null, channelNotifier);
                    pkg.changeStatus(stopping);

                    // stops running job
                    processRegistry.end(pkg);

                    Job freshCopy = jobsDao.retrieve(job.getJobUri());
                    return new ModelAndView(new JsonView()).addObject("json", freshCopy.getJsonDetail());
            }

            return resourceStateConflict(job, "job status must be [pending, scheduled, running]");
        }

        throw new ForbiddenAccessException(getUserUri(request));
    }

    @RequestMapping(value = "/**/jobs/*/delete", method = POST)
    @ModelAttribute
    public ModelAndView deleteJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        Job job = getJob(jobsDao, request, "/delete");
        if (isScriptOwner(request, job) || isScriptAdmin(request, job.getScriptUri(), scriptAdminsByUri)) {
            switch (job.getJobStatus()) {
                case completed:
                case stopped:
                case errored:
                    recursiveDelete(new File(job.getJobDirectory()));
                    jobsDao.delete(job);

                    return new ModelAndView(new OkResponseView());
            }

            return resourceStateConflict(job, "job status must be [completed, stopped, errored]");
        }

        throw new ForbiddenAccessException(getUserUri(request));
    }

    /*
    * Private Methods
    */

    private void scheduleJob(Job... jobs) throws IOException {
        if (jobs == null || jobs.length == 0) {
            log.info("no jobs scheduled");
            return;
        }

        for (Job job : jobs) {
            log.info(job.getJobUri());
            String scriptUri = job.getScriptUri();

            ReturnCodes returnCodes = returnCodesByUri.get(scriptUri);
            EmailBean emailBean = emailBeansByUri.get(scriptUri);

            mkdirs(new File(job.getExecutionDirectory()), new File(job.getOutputDirectoryPath()));

            JobPackage pkg = new JobPackage(job, jobsDao, returnCodes, channelNotifier, new EmailNotifier(emailBean));
            pkg.changeStatus(scheduled);

            Queue<JobPackage> q = jobQueuesByUri.get(scriptUri);
            q.add(pkg);
        }
    }

    private ModelAndView resourceStateConflict(Job job, String message) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uri", job.getJobUri());
        json.put("label", job.getLabel());
        json.put("status", job.getJobStatus());
        json.put("message", message);
        return new ModelAndView(new ResourceStateConflictView()).addObject("json", json);
    }

    private int getNumberOfThreads(String scriptUri) {
        try {
            return parseInt(numberOfThreadsByUri.get(scriptUri));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

}

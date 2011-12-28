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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.commons.web.views.ResourceStateConflictView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.IntegerPropertyByIdMappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategy;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategyMappingsHandler;
import org.systemsbiology.addama.services.execution.dao.JobsDaoAware;
import org.systemsbiology.addama.services.execution.jobs.Job;
import org.systemsbiology.addama.services.execution.jobs.JobPackage;
import org.systemsbiology.addama.services.execution.jobs.ReturnCodes;
import org.systemsbiology.addama.services.execution.jobs.ReturnCodesMappingsHandler;
import org.systemsbiology.addama.services.execution.notification.ChannelNotifier;
import org.systemsbiology.addama.services.execution.notification.EmailBean;
import org.systemsbiology.addama.services.execution.notification.EmailInstructionsMappingsHandler;
import org.systemsbiology.addama.services.execution.notification.EmailNotifier;
import org.systemsbiology.addama.services.execution.scheduling.JobQueueHandlingRunnable;
import org.systemsbiology.addama.services.execution.scheduling.JobQueuesMappingsHandler;
import org.systemsbiology.addama.services.execution.scheduling.ProcessRegistry;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringUtils.*;
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

    private final Map<String, ReturnCodes> returnCodesByToolId = new HashMap<String, ReturnCodes>();
    private final Map<String, Queue<JobPackage>> jobQueuesByToolId = new HashMap<String, Queue<JobPackage>>();
    private final Map<String, EmailBean> emailBeansByToolId = new HashMap<String, EmailBean>();
    private final Map<String, String> scriptAdminsByToolId = new HashMap<String, String>();
    private final Map<String, ArgumentStrategy> argumentStrategiesByToolId = new HashMap<String, ArgumentStrategy>();
    private final Map<String, String> workDirsByToolId = new HashMap<String, String>();
    private final Map<String, String> scriptsByToolId = new HashMap<String, String>();
    private final Map<String, String> logFilesByToolId = new HashMap<String, String>();
    private final Map<String, String> viewersByToolId = new HashMap<String, String>();
    private final Map<String, Integer> numberOfThreadsByToolId = new HashMap<String, Integer>();
    private final Map<String, String> jobExecutionDirectoryByUri = new HashMap<String, String>();
    private final Set<String> environmentVariables = new HashSet<String>();

    private final ProcessRegistry processRegistry = new ProcessRegistry();

    private ChannelNotifier channelNotifier;

    public void setChannelNotifier(ChannelNotifier channelNotifier) {
        this.channelNotifier = channelNotifier;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        super.setServiceConfig(serviceConfig);
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(workDirsByToolId, "workDir"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(scriptsByToolId, "script"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(logFilesByToolId, "logFile"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(viewersByToolId, "viewer"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(jobExecutionDirectoryByUri, "jobExecutionDirectory"));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(scriptAdminsByToolId, "scriptAdmin"));
        serviceConfig.visit(new IntegerPropertyByIdMappingsHandler(numberOfThreadsByToolId, "numberOfThreads", 1));
        serviceConfig.visit(new ReturnCodesMappingsHandler(returnCodesByToolId));
        serviceConfig.visit(new EmailInstructionsMappingsHandler(emailBeansByToolId));
        serviceConfig.visit(new JobQueuesMappingsHandler(jobQueuesByToolId));
        serviceConfig.visit(new ArgumentStrategyMappingsHandler(argumentStrategiesByToolId));

        JSONObject configuration = serviceConfig.JSON();
        if (configuration.has("environmentVars")) {
            JSONObject jsonVars = configuration.getJSONObject("environmentVars");

            Iterator keys = jsonVars.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                environmentVariables.add(key + "=" + jsonVars.getString(key));
            }
        }
    }

    public void afterPropertiesSet() throws Exception {
        jobsDao.resetJobs(running, pending);
        jobsDao.resetJobs(scheduled, pending);
        jobsDao.resetJobs(stopping, stopped);

        scheduleJob(jobsDao.retrievePendingJobs());

        String[] envVars = environmentVariables.toArray(new String[environmentVariables.size()]);

        for (Map.Entry<String, Queue<JobPackage>> entry : jobQueuesByToolId.entrySet()) {
            String toolId = entry.getKey();
            Queue<JobPackage> jpq = entry.getValue();

            for (int j = 0; j < numberOfThreadsByToolId.get(toolId); j++) {
                new Thread(new JobQueueHandlingRunnable(jpq, processRegistry, envVars)).start();
            }
        }
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs", method = RequestMethod.POST)
    public ModelAndView executeJob(HttpServletRequest request, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);
        toolExists(toolId);

        String jobId = randomUUID().toString();
        String jobUri = chomp(substringAfterLast(request.getRequestURI(), request.getContextPath()), "/") + "/" + jobId;
        String workDir = workDirsByToolId.get(toolId) + "/jobs/" + jobId;
        String scriptPath = scriptsByToolId.get(toolId);

        Job job = new Job(jobUri, toolId, getUserUri(request), workDir, scriptPath);
        job.setExecutionDirectoryFromConfiguration(jobExecutionDirectoryByUri.get(toolId));

        // WARNING:  This logic may need to obtain inputstream from request... DO NOT call request.getParameter before this logic, or it will not work!
        // Major design flaw in Servlet Spec or Tomcat... not sure:  https://issues.apache.org/bugzilla/show_bug.cgi?id=47410
        argumentStrategiesByToolId.get(toolId).handle(job, request);

        job.setLabel(request.getParameter("label"));
        job.setChannelUri(getChannelUri(request));

        jobsDao.create(job);

        scheduleJob(job);

        return new ModelAndView(new JsonView()).addObject("json", job.getJsonSummary());
    }

    @RequestMapping(value = "/**/tools/jobs", method = RequestMethod.GET)
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

    @RequestMapping(value = "/**/tools/{toolId}/jobs", method = RequestMethod.GET)
    public ModelAndView getJobs(HttpServletRequest request, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);

        Job[] jobs;
        if (isScriptAdmin(request, toolId, scriptAdminsByToolId)) {
            jobs = jobsDao.retrieveAllForScript(toolId);
        } else {
            jobs = jobsDao.retrieveAllForScript(toolId, getUserUri(request));
        }

        JSONObject json = new JSONObject();
        for (Job job : jobs) {
            json.append("items", job.getJsonSummary());
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/jobs/{jobId}", method = RequestMethod.GET)
    public ModelAndView getJobById(HttpServletRequest request, @PathVariable("jobId") String jobId) throws Exception {
        log.fine(jobId);

        String jobUri = chomp(substringAfterLast(request.getRequestURI(), request.getContextPath()), "/");

        Job job = jobsDao.retrieve(jobUri);
        if (job == null) {
            throw new ResourceNotFoundException(jobId);
        }

        return new ModelAndView(new JsonView()).addObject("json", job.getJsonDetail());
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/stop", method = POST)
    public ModelAndView stopJob(HttpServletRequest request, @PathVariable("toolId") String toolId,
                                @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        String jobUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/stop");
        Job job = jobsDao.retrieve(jobUri);
        if (job == null) {
            throw new ResourceNotFoundException(jobId);
        }

        if (isScriptOwner(request, job) || isScriptAdmin(request, toolId, scriptAdminsByToolId)) {
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

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/delete", method = POST)
    public ModelAndView deleteJob(HttpServletRequest request, @PathVariable("toolId") String toolId,
                                  @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        String jobUri = substringBetween(request.getRequestURI(), request.getContextPath(), "/delete");
        Job job = jobsDao.retrieve(jobUri);
        if (job == null) {
            throw new ResourceNotFoundException(jobId);
        }

        if (isScriptOwner(request, job) || isScriptAdmin(request, toolId, scriptAdminsByToolId)) {
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

            ReturnCodes returnCodes = returnCodesByToolId.get(scriptUri);
            EmailBean emailBean = emailBeansByToolId.get(scriptUri);

            mkdirs(new File(job.getExecutionDirectory()), new File(job.getOutputDirectoryPath()));

            JobPackage pkg = new JobPackage(job, jobsDao, returnCodes, channelNotifier, new EmailNotifier(emailBean));
            pkg.changeStatus(scheduled);

            jobQueuesByToolId.get(scriptUri).add(pkg);
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

    private void toolExists(String toolId) throws ResourceNotFoundException {
        if (scriptsByToolId.containsKey(toolId)) {
            String script = scriptsByToolId.get(toolId);
            if (!isEmpty(script)) {
                return;
            }
        }
        throw new ResourceNotFoundException(toolId);
    }

}

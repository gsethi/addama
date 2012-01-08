package org.systemsbiology.addama.services.execution.mvc;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceStateConflictException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.OkResponseView;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.jsonconfig.impls.CollectIdsMappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.IntegerPropertyByIdMappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.StringPropertyByIdMappingsHandler;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategy;
import org.systemsbiology.addama.services.execution.args.ArgumentStrategyMappingsHandler;
import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.InMemoryJobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.JdbcTemplateJobsDao;
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
import org.systemsbiology.google.visualization.datasource.impls.AbstractDataTableGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.apache.commons.lang.StringUtils.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.*;
import static org.systemsbiology.addama.services.execution.jobs.JobStatus.*;
import static org.systemsbiology.addama.services.execution.util.HttpJob.*;
import static org.systemsbiology.addama.services.execution.util.IOJob.*;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.getDataTableGeneratorByOutputType;
import static org.systemsbiology.google.visualization.datasource.JdbcTemplateHelper.getJdbcTemplate;

/**
 * @author hrovira
 */
@Controller
public class MainController implements InitializingBean {
    private static final Logger log = Logger.getLogger(MainController.class.getName());

    private final HashSet<String> toolIds = new HashSet<String>();
    private final Map<String, String> viewersById = new HashMap<String, String>();
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

    private JobsDao jobsDao = new InMemoryJobsDao();
    private ChannelNotifier channelNotifier;


    public void setChannelNotifier(ChannelNotifier channelNotifier) {
        this.channelNotifier = channelNotifier;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        JSONObject item = serviceConfig.JSON();
        if (item.has("jobsDb")) {
            JSONObject jobsDb = item.getJSONObject("jobsDb");
            jobsDb.put("defaultAutoCommit", true);
            this.jobsDao = new JdbcTemplateJobsDao(getJdbcTemplate(jobsDb));
        }

        serviceConfig.visit(new CollectIdsMappingsHandler(toolIds));
        serviceConfig.visit(new StringPropertyByIdMappingsHandler(viewersById, "viewer"));
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

    /*
     * GET Controllers
     */
    @RequestMapping(value = "/**/myjobs", method = RequestMethod.GET)
    public ModelAndView jobs_for_user(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String userUri = getUserUri(request);
        Job[] jobs = jobsDao.retrieveAllForUser(userUri);

        JSONObject json = new JSONObject();
        for (Job job : jobs) {
            json.append("items", job.getJsonSummary());
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/tools", method = RequestMethod.GET)
    public ModelAndView tools(HttpServletRequest request) throws Exception {
        String uri = getURI(request);
        log.info(uri);

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        for (String toolId : toolIds) {
            json.append("items", getToolJson(toolId, uri + "/" + toolId));
        }
        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    @RequestMapping(value = "/**/tools/{toolId}", method = RequestMethod.GET)
    public ModelAndView tools_by_id(HttpServletRequest request, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);

        String uri = getURI(request);
        return new ModelAndView(new JsonView()).addObject("json", getToolJson(toolId, uri));
    }

    @RequestMapping(value = "/**/tools/{toolId}/ui", method = RequestMethod.GET)
    public void tool_ui(HttpServletResponse response, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);

        if (!viewersByToolId.containsKey(toolId)) {
            throw new ResourceNotFoundException(toolId);
        }

        clientRedirect(response, viewersByToolId.get(toolId));
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs", method = RequestMethod.GET)
    public ModelAndView tools_jobs(HttpServletRequest request, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);

        Job[] jobs;
        if (isScriptAdmin(request, toolId)) {
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

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}", method = RequestMethod.GET)
    public ModelAndView jobs_by_id(@PathVariable("toolId") String toolId, @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);
        return new ModelAndView(new JsonView()).addObject("json", job.getJsonDetail());
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/log", method = RequestMethod.GET)
    public void job_log(HttpServletResponse response, @PathVariable("toolId") String toolId,
                        @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        File f = new File(job.getLogPath());
        if (!f.exists()) {
            response.setStatus(SC_NO_CONTENT);
            return;
        }

        String contents = getLogContents(f);
        if (isEmpty(contents)) {
            response.setStatus(SC_NO_CONTENT);
            return;
        }

        response.setContentType("text/plain");
        response.getWriter().write(contents);
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/outputs", method = RequestMethod.GET)
    public ModelAndView job_outputs(@PathVariable("toolId") String toolId, @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        return new ModelAndView(new JsonItemsView()).addObject("json", job.getJsonOutputs());
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/outputs/**", method = RequestMethod.GET)
    public ModelAndView job_output_by_path(HttpServletRequest request, HttpServletResponse response,
                                           @PathVariable("toolId") String toolId, @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        String filepath = substringAfter(request.getRequestURI(), "/outputs/");
        if (contains(filepath, "_afdl/")) filepath = substringAfter(filepath, "_afdl/");
        if (contains(filepath, "_afdl")) filepath = substringAfter(filepath, "_afdl");

        if (!isEmpty(filepath)) {
            outputFile(job, filepath, response, request.getSession().getServletContext());
            return new ModelAndView(new OkResponseView());
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", job.getJsonDetail());
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/outputs/**/query", method = RequestMethod.GET)
    public void job_output_query(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("toolId") String toolId, @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        File outputDir = new File(job.getOutputDirectoryPath());
        if (!outputDir.exists()) {
            throw new ResourceNotFoundException(job.getJobUri() + "/outputs");
        }

        String filepath = substringBetween(request.getRequestURI(), "/outputs", "/query");
        log.fine("filepath=" + filepath);
        if (!isEmpty(filepath)) {
            File queryFile = new File(outputDir + filepath);
            InputStream checkStream = new FileInputStream(queryFile);
            InputStream inputStream = new FileInputStream(queryFile);

            try {
                AbstractDataTableGenerator dataTableGenerator = getDataTableGeneratorByOutputType(checkStream, inputStream);
                if (dataTableGenerator == null) {
                    throw new InvalidSyntaxException("file cannot be queried");
                }

                executeDataSourceServletFlow(request, response, dataTableGenerator);
            } catch (Exception e) {
                log.warning("queryJobOutput(" + request.getRequestURI() + "):" + e);
            } finally {
                checkStream.close();
                inputStream.close();
            }
        }
    }

    /*
     * POST Controllers
     */
    @RequestMapping(value = "/**/tools/{toolId}/jobs", method = RequestMethod.POST)
    public ModelAndView job_execute(HttpServletRequest request, @PathVariable("toolId") String toolId) throws Exception {
        log.info(toolId);

        String scriptPath = scriptsByToolId.get(toolId);
        if (isEmpty(scriptPath)) {
            throw new ResourceNotFoundException(toolId);
        }

        String jobId = randomUUID().toString();
        String workDir = workDirsByToolId.get(toolId) + "/jobs/" + jobId;

        Job job = new Job(jobId, toolId, getUserUri(request), workDir, scriptPath);
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

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/stop", method = POST)
    public ModelAndView job_stop(HttpServletRequest request, @PathVariable("toolId") String toolId,
                                 @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        if (isScriptOwner(request, job) || isScriptAdmin(request, toolId)) {
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

            throw new ResourceStateConflictException("job status must be [pending, scheduled, running]");
        }

        throw new ForbiddenAccessException(getUserUri(request));
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/delete", method = POST)
    public ModelAndView job_delete(HttpServletRequest request, @PathVariable("toolId") String toolId,
                                   @PathVariable("jobId") String jobId) throws Exception {
        log.info(toolId + ":" + jobId);

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        if (isScriptOwner(request, job) || isScriptAdmin(request, toolId)) {
            switch (job.getJobStatus()) {
                case completed:
                case stopped:
                case errored:
                    recursiveDelete(new File(job.getJobDirectory()));
                    jobsDao.delete(job);

                    return new ModelAndView(new OkResponseView());
            }

            throw new ResourceStateConflictException("job status must be [completed, stopped, errored]");
        }

        throw new ForbiddenAccessException(getUserUri(request));
    }

    @RequestMapping(value = "/**/tools/{toolId}/jobs/{jobId}/outputs", method = RequestMethod.GET)
    public ModelAndView job_output_zip(HttpServletResponse response, @PathVariable("toolId") String toolId,
                                       @PathVariable("jobId") String jobId,
                                       @RequestParam(value = "name", required = false) String name,
                                       @RequestParam(value = "uris", required = false) String[] fileUris) throws Exception {
        log.info(toolId + ":" + jobId);

        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();

        Job job = jobsDao.retrieve(jobId);
        if (job == null) throw new ResourceNotFoundException(jobId);

        if (isEmpty(name) && (fileUris == null || fileUris.length == 0)) {
            for (File f : getOutputFiles(job)) {
                inputStreamsByName.put(f.getName(), new FileInputStream(f));
            }
            String label = job.getLabel();
            if (isEmpty(label)) {
                label = chomp(substringAfterLast(job.getJobUri(), "/"), "/");
            }
            zip(response, label + ".zip", inputStreamsByName);
            return new ModelAndView(new OkResponseView());
        }

        for (String fileUri : fileUris) {
            String filepath = substringAfter(fileUri, "/outputs");
            if (contains(filepath, "_afdl/")) filepath = substringAfter(filepath, "_afdl/");
            if (contains(filepath, "_afdl")) filepath = substringAfter(filepath, "_afdl");

            if (!isEmpty(filepath)) {
                File outputFile = new File(job.getOutputDirectoryPath(), filepath);
                if (outputFile.exists()) {
                    String path = job.getLabel();
                    if (isEmpty(path)) {
                        path = chomp(substringAfterLast(job.getJobUri(), "/"), "/");
                    }
                    inputStreamsByName.put(path + "/outputs/" + filepath, new FileInputStream(outputFile));
                }
            }
        }
        zip(response, name + ".zip", inputStreamsByName);
        return new ModelAndView(new OkResponseView());
    }

    /*
     * Private Methods
     */
    private JSONObject getToolJson(String toolId, String uri) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", toolId);
        json.put("uri", uri);
        json.put("jobs", uri + "/jobs");
        if (viewersById.containsKey(toolId)) {
            json.put("ui", uri + "/ui");
        }
        return json;
    }

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

    private boolean isScriptAdmin(HttpServletRequest request, String toolId) {
        String userUri = getUserUri(request);
        if (scriptAdminsByToolId.containsKey(toolId)) {
            String scriptAdmin = scriptAdminsByToolId.get(toolId);
            if (!isEmpty(scriptAdmin)) {
                String user = substringAfter(userUri, "/addama/users/");
                return equalsIgnoreCase(scriptAdmin, user);
            }
        }
        return false;
    }


}

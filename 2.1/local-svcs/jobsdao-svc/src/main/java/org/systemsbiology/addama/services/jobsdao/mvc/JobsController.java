package org.systemsbiology.addama.services.jobsdao.mvc;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.ResourceStateConflictView;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;
import org.systemsbiology.addama.services.jobsdao.dao.InMemoryJobsDao;
import org.systemsbiology.addama.services.jobsdao.dao.JdbcTemplateJobsDao;
import org.systemsbiology.addama.services.jobsdao.dao.JobsDao;
import org.systemsbiology.addama.services.jobsdao.pojo.Job;
import static org.systemsbiology.addama.services.jobsdao.util.HttpJob.getJob;
import static org.systemsbiology.addama.services.jobsdao.util.HttpJob.getUserUri;
import static org.systemsbiology.google.visualization.datasource.JdbcTemplateHelper.getJdbcTemplate;

import javax.servlet.http.HttpServletRequest;
import static java.util.UUID.randomUUID;
import java.util.logging.Logger;

/**
 * @author: hrovira
 */
@Controller
public class JobsController {
    private static final Logger log = Logger.getLogger(JobsController.class.getName());

    private JobsDao jobsDao = new InMemoryJobsDao();

    public void setJobsDao(JobsDao jobsDao) {
        this.jobsDao = jobsDao;
    }

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new JsonConfigHandler() {
            public void handle(JSONObject item) throws Exception {
                if (item.has("jobsDb")) {
                    JSONObject jobsDb = item.getJSONObject("jobsDb");
                    jobsDb.put("defaultAutoCommit", true);
                    jobsDao = new JdbcTemplateJobsDao(getJdbcTemplate(jobsDb));
                }
            }
        });
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView executeJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String jobId = randomUUID().toString();
        String jobUri = "/jobsdao-svc/jobs/" + jobId;

        Job job = new Job(jobUri, getUserUri(request));
        job.setLabel(request.getParameter("label"));

        jobsDao.create(job);

        // TODO : Schedule Job?

        return new ModelAndView(new JsonView()).addObject("json", job.getJsonSummary());
    }

    @RequestMapping(value = "/**/jobs", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJobs(HttpServletRequest request) throws Exception {
        log.fine(request.getRequestURI());

        Job[] jobs = jobsDao.retrieveAll();

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

    /*
    * Private Methods
    */

    private ModelAndView resourceStateConflict(Job job, String message) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uri", job.getJobUri());
        json.put("label", job.getLabel());
        json.put("status", job.getJobStatus());
        json.put("message", message);
        return new ModelAndView(new ResourceStateConflictView()).addObject("json", json);
    }

}

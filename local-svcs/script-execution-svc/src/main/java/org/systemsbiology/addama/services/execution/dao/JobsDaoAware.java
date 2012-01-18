package org.systemsbiology.addama.services.execution.dao;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;
import org.systemsbiology.addama.services.execution.dao.impls.InMemoryJobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.JdbcTemplateJobsDao;

import static org.systemsbiology.google.visualization.datasource.JdbcTemplateHelper.getJdbcTemplate;

/**
 * @author hrovira
 */
public class JobsDaoAware {
    protected JobsDao jobsDao = new InMemoryJobsDao();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        JSONObject item = serviceConfig.JSON();
        if (item.has("jobsDb")) {
            JSONObject jobsDb = item.getJSONObject("jobsDb");
            jobsDb.put("defaultAutoCommit", true);
            this.jobsDao = new JdbcTemplateJobsDao(getJdbcTemplate(jobsDb));
        }
    }
}

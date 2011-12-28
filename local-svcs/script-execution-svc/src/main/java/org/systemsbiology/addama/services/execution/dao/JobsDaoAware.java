package org.systemsbiology.addama.services.execution.dao;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
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
        serviceConfig.visit(new MappingsHandler() {
            public void handle(Mapping mapping) throws Exception {
                JSONObject item = mapping.JSON();
                if (item.has("jobsDb")) {
                    JSONObject jobsDb = item.getJSONObject("jobsDb");
                    jobsDb.put("defaultAutoCommit", true);
                    jobsDao = new JdbcTemplateJobsDao(getJdbcTemplate(jobsDb));
                }
            }
        });
    }
}

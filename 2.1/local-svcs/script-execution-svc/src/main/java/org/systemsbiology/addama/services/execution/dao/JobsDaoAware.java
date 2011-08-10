package org.systemsbiology.addama.services.execution.dao;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.JsonConfig;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;
import org.systemsbiology.addama.services.execution.dao.impls.InMemoryJobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.JdbcTemplateJobsDao;

import static org.systemsbiology.google.visualization.datasource.JdbcTemplateHelper.getJdbcTemplate;

/**
 * @author hrovira
 */
public class JobsDaoAware {
    protected JobsDao jobsDao = new InMemoryJobsDao();

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
}

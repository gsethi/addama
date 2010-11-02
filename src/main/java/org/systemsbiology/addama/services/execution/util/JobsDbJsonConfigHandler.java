/*
 * Copyright (C) 2003-2010 Institute for Systems Biology
 *                             Seattle, Washington, USA.
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package org.systemsbiology.addama.services.execution.util;

import org.apache.commons.dbcp.BasicDataSource;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.systemsbiology.addama.registry.JsonConfigHandler;
import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.InMemoryJobsDao;
import org.systemsbiology.addama.services.execution.dao.impls.JdbcTemplateJobsDao;

/**
 * @author hrovira
 */
public class JobsDbJsonConfigHandler implements JsonConfigHandler {
    private JobsDao jobsDao = new InMemoryJobsDao();

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("jobsDb")) {
            JSONObject jobsDb = configuration.getJSONObject("jobsDb");

            BasicDataSource bds = new BasicDataSource();
            bds.setDefaultAutoCommit(false);
            bds.setDriverClassName(jobsDb.getString("classname"));
            bds.setUrl(jobsDb.getString("jdbcurl"));
            if (jobsDb.has("username")) {
                bds.setUsername(jobsDb.getString("username"));
            }
            if (jobsDb.has("password")) {
                bds.setPassword(jobsDb.getString("password"));
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(bds);
            jdbcTemplate.afterPropertiesSet();

            this.jobsDao = new JdbcTemplateJobsDao(jdbcTemplate);
        }
    }

    public JobsDao getJobsDao() {
        return jobsDao;
    }
}

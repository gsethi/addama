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

package org.systemsbiology.addama.services.execution.dao.impls;

import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.services.execution.dao.Job;
import org.systemsbiology.addama.services.execution.dao.JobsDao;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author hrovira
 */
public class InMemoryJobsDao implements JobsDao {
    private static final HashMap<String, Job> jobsByUri = new HashMap<String, Job>();

    public Job retrieve(String jobUri) {
        return jobsByUri.get(jobUri);
    }


    public void create(Job job) {
        jobsByUri.put(job.getJobUri(), job);
    }


    public void modify(Job job) {
        if (!jobsByUri.containsKey(job.getJobUri())) {
            create(job);
        }
    }


    public Job[] retrieveAllForScript(String scriptUri) {
        ArrayList<Job> matching = new ArrayList<Job>();
        for (Job job : jobsByUri.values()) {
            if (StringUtils.equalsIgnoreCase(job.getScriptUri(), scriptUri)) {
                matching.add(job);
            }

        }
        return matching.toArray(new Job[matching.size()]);
    }


    public Job[] retrieveAllForScript(String scriptUri, String userUri) {
        ArrayList<Job> matching = new ArrayList<Job>();
        for (Job job : retrieveAllForScript(scriptUri)) {
            if (StringUtils.equalsIgnoreCase(job.getUserUri(), userUri)) {
                matching.add(job);
            }

        }
        return matching.toArray(new Job[matching.size()]);
    }


    public Job[] retrieveAllForUser(String userUri) {
        ArrayList<Job> matching = new ArrayList<Job>();
        for (Job job : jobsByUri.values()) {
            if (StringUtils.equalsIgnoreCase(job.getUserUri(), userUri)) {
                matching.add(job);
            }

        }
        return matching.toArray(new Job[matching.size()]);
    }
}

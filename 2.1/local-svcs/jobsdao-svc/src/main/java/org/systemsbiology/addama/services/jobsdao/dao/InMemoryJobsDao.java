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

package org.systemsbiology.addama.services.jobsdao.dao;

import org.systemsbiology.addama.services.jobsdao.pojo.Job;
import static org.systemsbiology.addama.services.jobsdao.pojo.JobStatus.pending;

import java.util.ArrayList;
import java.util.Collection;
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

    public void update(Job job) {
        if (!jobsByUri.containsKey(job.getJobUri())) {
            create(job);
        }
    }

    public void delete(Job job) {
        jobsByUri.remove(job.getJobUri());
    }

    public Job[] retrieveAll() {
        Collection<Job> jobs = jobsByUri.values();
        return jobs.toArray(new Job[jobs.size()]);
    }

    public Job[] retrievePendingJobs() {
        ArrayList<Job> matching = new ArrayList<Job>();
        for (Job job : jobsByUri.values()) {
            if (pending.equals(job.getJobStatus())) {
                matching.add(job);
            }

        }
        return matching.toArray(new Job[matching.size()]);
    }
}
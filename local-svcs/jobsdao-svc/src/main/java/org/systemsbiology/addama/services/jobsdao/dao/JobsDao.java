package org.systemsbiology.addama.services.jobsdao.dao;

import org.systemsbiology.addama.services.jobsdao.pojo.Job;

/**
 * @author: hrovira
 */
public interface JobsDao {
    public void create(Job job);

    public Job retrieve(String jobUri);

    public Job[] retrieveAll();

    public Job[] retrievePendingJobs();

    public void update(Job job);

    public void delete(Job job);


}

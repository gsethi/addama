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
package org.systemsbiology.addama.services.execution.jobs;

import org.systemsbiology.addama.services.execution.dao.Job;
import org.systemsbiology.addama.services.execution.dao.JobStatus;

import java.io.Closeable;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class ResultsExecutionCallback implements ExecutionCallback {
    private static final Logger log = Logger.getLogger(ResultsExecutionCallback.class.getName());

    private final Job job;
    private final Closeable[] closeables;

    public ResultsExecutionCallback(Job job, Closeable... closeables) {
        this.job = job;
        this.closeables = closeables;
    }

    public void onResult(Process process, int result) {
        job.setJobStatus(JobStatus.completed);
        closeResources();
    }

    public void onError(Exception e) {
        job.setJobStatus(JobStatus.errored);
        job.setErrorMessage(e.getMessage());
        closeResources();
    }

    /*
     * Private Methods
     */

    private void closeResources() {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    log.warning("endOfProcess:" + e);
                }
            }
        }
    }
}

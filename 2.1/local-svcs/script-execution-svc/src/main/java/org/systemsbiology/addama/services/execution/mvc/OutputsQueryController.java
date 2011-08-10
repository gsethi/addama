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
package org.systemsbiology.addama.services.execution.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.services.execution.dao.JobsDaoAware;
import org.systemsbiology.addama.services.execution.jobs.Job;
import org.systemsbiology.google.visualization.datasource.impls.AbstractDataTableGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringBetween;
import static org.systemsbiology.addama.services.execution.util.HttpJob.getJob;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.executeDataSourceServletFlow;
import static org.systemsbiology.google.visualization.datasource.DataSourceHelper.getDataTableGeneratorByOutputType;

/**
 * @author hrovira
 */
@Controller
public class OutputsQueryController extends JobsDaoAware {
    private static final Logger log = Logger.getLogger(OutputsQueryController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    public void queryJobOutput(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Job job = getJob(jobsDao, request, "/outputs");

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

}
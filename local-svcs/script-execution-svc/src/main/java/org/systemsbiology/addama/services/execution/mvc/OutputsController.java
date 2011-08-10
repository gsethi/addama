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
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.services.execution.dao.JobsDaoAware;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.apache.commons.lang.StringUtils.contains;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.systemsbiology.addama.services.execution.util.HttpJob.getJob;

/**
 * @author hrovira
 */
@Controller
public class OutputsController extends JobsDaoAware implements ServletContextAware {
    private ServletContext servletContext;
    private int bufferSize = 8096;

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @RequestMapping(value = "/**/outputs/**", method = RequestMethod.GET)
    public ModelAndView getJobOutput(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Job job = getJob(jobsDao, request, "/outputs");

        String filepath = substringAfter(request.getRequestURI(), "/outputs/");
        if (contains(filepath, "_afdl/")) filepath = substringAfter(filepath, "_afdl/");
        if (contains(filepath, "_afdl")) filepath = substringAfter(filepath, "_afdl");

        if (!isEmpty(filepath)) {
            outputFile(job, filepath, response);
            return null;
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", job.getJsonDetail());
    }

    /*
     * Private Methods
     */

    private void outputFile(Job job, String filepath, HttpServletResponse response)
            throws ResourceNotFoundException, IOException {
        File outputDir = new File(job.getOutputDirectoryPath());
        if (!outputDir.exists()) {
            throw new ResourceNotFoundException(job.getJobUri() + "/outputs");
        }

        File outputFile = new File(job.getOutputDirectoryPath(), filepath);
        if (!outputFile.exists()) {
            throw new ResourceNotFoundException(job.getJobUri() + "/outputs/_afdl/" + filepath);
        }

        response.setContentType(servletContext.getMimeType(outputFile.getName()));

        InputStream inputStream = new FileInputStream(outputFile);
        OutputStream outputStream = response.getOutputStream();

        byte[] buffer = new byte[this.bufferSize];
        while (true) {
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            outputStream.write(buffer, 0, bytesRead);
        }
    }
}
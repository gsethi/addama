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

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class LogsController extends BaseController {
    private static final Logger log = Logger.getLogger(LogsController.class.getName());

    private String jobPath;

    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }

    @RequestMapping(method = RequestMethod.GET)
    public void getJobLog(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jobUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/log");
        log.info("getJobLog(" + request.getRequestURI() + "):" + jobUri);

        String scriptUri = StringUtils.substringBefore(jobUri, jobPath);

        String workDir = workDirsByUri.get(scriptUri);
        if (StringUtils.isEmpty(workDir)) {
            throw new ResourceNotFoundException("work directory for " + scriptUri);
        }

        String jobId = StringUtils.substringAfter(jobUri, jobPath);
        String contents = getLogContents(workDir + jobPath + jobId + "/job.log");
        if (StringUtils.isEmpty(contents)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        response.setContentType("text/plain");
        response.getWriter().write(contents);
    }

    /*
     * Private Methods
     */

    private String getLogContents(String logFile) {
        StringBuilder builder = new StringBuilder();
        BufferedReader logReader = null;
        try {
            logReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
            String line = "";
            while (line != null) {
                line = logReader.readLine();
                if (line != null) {
                    builder.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            log.warning("getLogContents(" + logFile + "):" + e);
        } finally {
            try {
                if (logReader != null) {
                    logReader.close();
                }
            } catch (IOException e) {
                log.warning("getLogContents(" + logFile + "):" + e);
            }
        }
        return builder.toString();
    }


}
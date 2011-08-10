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
import org.springframework.web.bind.annotation.RequestParam;
import org.systemsbiology.addama.services.execution.dao.JobsDaoAware;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.zip;
import static org.systemsbiology.addama.services.execution.util.HttpJob.*;

/**
 * @author hrovira
 */
@Controller
public class OutputsZipController extends JobsDaoAware {
    private static final Logger log = Logger.getLogger(OutputsZipController.class.getName());

    @RequestMapping(value = "/**/outputs/zip", method = RequestMethod.POST)
    public void zipJobOutput(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "uris", required = false) String[] fileUris) throws Exception {
        log.info(request.getRequestURI());

        Map<String, InputStream> inputStreamsByName = new HashMap<String, InputStream>();

        Job job = getJob(jobsDao, request, "/outputs");
        if (isEmpty(name) && (fileUris == null || fileUris.length == 0)) {
            for (File f : getOutputFiles(job)) {
                inputStreamsByName.put(f.getName(), new FileInputStream(f));
            }
            String label = job.getLabel();
            if (isEmpty(label)) {
                label = chomp(substringAfterLast(job.getJobUri(), "/"), "/");
            }
            zip(response, label + ".zip", inputStreamsByName);
            return;
        }

        for (String fileUri : fileUris) {
            String filepath = substringAfter(fileUri, "/outputs");
            if (contains(filepath, "_afdl/")) filepath = substringAfter(filepath, "_afdl/");
            if (contains(filepath, "_afdl")) filepath = substringAfter(filepath, "_afdl");

            if (!isEmpty(filepath)) {
                File outputFile = new File(job.getOutputDirectoryPath(), filepath);
                if (outputFile.exists()) {
                    String path = job.getLabel();
                    if (isEmpty(path)) {
                        path = chomp(substringAfterLast(job.getJobUri(), "/"), "/");
                    }
                    inputStreamsByName.put(path + "/outputs/" + filepath, new FileInputStream(outputFile));
                }
            }
        }
        zip(response, name + ".zip", inputStreamsByName);
    }
}
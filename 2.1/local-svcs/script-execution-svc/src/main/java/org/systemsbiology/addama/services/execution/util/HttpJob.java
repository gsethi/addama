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
package org.systemsbiology.addama.services.execution.util;

import org.json.JSONException;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.*;
import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;

/**
 * @author hrovira
 */
public class HttpJob {
    private static final Logger log = Logger.getLogger(HttpJob.class.getName());

    public static String getUserEmail(HttpServletRequest request) {
        String userUri = getUserUri(request);
        if (isEmpty(userUri)) {
            return null;
        }
        return substringAfterLast(userUri, "/addama/users/");
    }

    public static File[] getOutputFiles(Job job) throws JSONException {
        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, new File(job.getOutputDirectoryPath()));
        return outputs.toArray(new File[outputs.size()]);
    }

    public static void scanOutputs(List<File> outputs, File outputDir) {
        if (outputDir.isFile()) {
            outputs.add(outputDir);
        }
        if (outputDir.isDirectory()) {
            for (File f : outputDir.listFiles()) {
                scanOutputs(outputs, f);
            }
        }
    }

    public static String getUserUri(HttpServletRequest request) {
        String userUri = request.getHeader("x-addama-registry-user");
        if (!isEmpty(userUri)) {
            return userUri;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (equalsIgnoreCase("x-addama-registry-user", cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public static Job getJob(JobsDao jobsDao, HttpServletRequest request, String suffix) throws ResourceNotFoundException {
        String jobUri = substringAfter(request.getRequestURI(), request.getContextPath());
        if (!isEmpty(suffix)) {
            jobUri = substringBetween(request.getRequestURI(), request.getContextPath(), suffix);
        }
        log.fine(jobUri);
        Job job = jobsDao.retrieve(jobUri);
        if (job == null) {
            throw new ResourceNotFoundException(jobUri);
        }
        return job;
    }

    public static Job getJobByUri(JobsDao jobsDao, HttpServletRequest request, String requestedUri, String suffix) {
        String jobUri = substringAfter(requestedUri, request.getContextPath());
        if (!isEmpty(suffix)) {
            jobUri = substringBetween(requestedUri, request.getContextPath(), suffix);
        }
        log.fine(jobUri);
        return jobsDao.retrieve(jobUri);
    }

    public static String getChannelUri(HttpServletRequest request) {
        String channelUri = getStringParameter(request, "x-addama-preferred-channel", null);
        if (!isEmpty(channelUri)) {
            return channelUri;
        }

        String userUri = getUserUri(request);
        if (!isEmpty(userUri)) {
            return replace(userUri, "/addama/users/", "/addama/channels/");
        }
        return null;
    }

    public static boolean isScriptOwner(HttpServletRequest request, Job job) {
        String userUri = getUserUri(request);
        return equalsIgnoreCase(job.getUserUri(), userUri);
    }

    public static boolean isScriptAdmin(HttpServletRequest request, String scriptUri, Map<String, String> scriptAdminsByUri) {
        String userUri = getUserUri(request);
        if (scriptAdminsByUri.containsKey(scriptUri)) {
            String scriptAdmin = scriptAdminsByUri.get(scriptUri);
            if (!isEmpty(scriptAdmin)) {
                String user = substringAfter(userUri, "/addama/users/");
                return equalsIgnoreCase(scriptAdmin, user);
            }
        }
        return false;
    }

    public static String[] getScriptExecution(Job job) {
        ArrayList<String> exec = new ArrayList<String>();
        String scriptPath = job.getScriptPath();
        if (!isEmpty(scriptPath)) {
            exec.addAll(asList(job.getScriptPath().split(" ")));
        }
        if (!isEmpty(job.getScriptArgs())) {
            exec.add(job.getScriptArgs());
        }
        log.fine("args=" + exec);
        return exec.toArray(new String[exec.size()]);
    }

    public static String getScriptUri(HttpServletRequest request, String suffix) throws ResourceNotFoundException {
        String scriptUri = substringAfter(request.getRequestURI(), request.getContextPath());
        if (!isEmpty(suffix)) {
            scriptUri = substringBetween(request.getRequestURI(), request.getContextPath(), suffix);
        }

        log.fine(scriptUri);
        return scriptUri;
    }

    public static void scriptExists(String scriptUri, Map<String, String>... maps) throws ResourceNotFoundException {
        for (Map<String, String> map : maps) {
            if (!map.containsKey(scriptUri)) {
                throw new ResourceNotFoundException(scriptUri);
            }
        }
    }
}
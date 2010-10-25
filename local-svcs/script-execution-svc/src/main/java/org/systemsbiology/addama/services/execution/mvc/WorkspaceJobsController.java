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
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.commons.web.views.ResourceNotFoundView;
import org.systemsbiology.addama.services.execution.io.Streamer;
import org.systemsbiology.addama.services.execution.jobs.ExecutionRunnable;
import org.systemsbiology.addama.services.execution.jobs.Results;
import org.systemsbiology.addama.services.execution.jobs.Workspace;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
@Controller
public class WorkspaceJobsController extends BaseController {
    private static final Logger log = Logger.getLogger(WorkspaceJobsController.class.getName());

    private final HashMap<String, Results> resultsByUri = new HashMap<String, Results>();

    private HttpClientTemplate httpClientTemplate;

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    @RequestMapping(value = "/**/workspace/jobs", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView executeJob(HttpServletRequest request, @RequestParam("workspace") String workspaceUri) throws Exception {
        log.info(request.getRequestURI());

        String userEmail = getUserEmail(request);
        if (StringUtils.isEmpty(userEmail)) {
            userEmail = "anonymous";
        }
        
        String scriptUri = getScriptUri(request);

        String jobId = userEmail + "/" + UUID.randomUUID().toString();
        String jobUri = scriptUri + "/workspace/jobs/" + jobId;
        String script = scriptsByUri.get(scriptUri) + " " + workspaceUri + " " + getQueryString(request);
        String jobDir = workDirsByUri.get(scriptUri) + "/workspace/jobs/" + jobId;

        Results r = executeRobot(jobUri, workspaceUri, script, jobDir);

        resultsByUri.put(jobUri, r);

        JSONObject json = new JSONObject();
        json.put("uri", jobUri);

        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        return mav;
    }

    @RequestMapping(method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView getJob(HttpServletRequest request) throws Exception {
        log.info(request.getRequestURI());

        String uri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        if (uri.endsWith("/workspace/jobs")) {
            String scriptUri = getScriptUri(request);

            JSONObject json = new JSONObject();

            for (String jobUri : resultsByUri.keySet()) {
                if (jobUri.startsWith(scriptUri)) {
                    json.append("items", new JSONObject().put("uri", jobUri));
                }
            }

            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }

        String[] resultUris = getMatchingResultUris(uri);
        if (resultUris != null && resultUris.length > 0) {
            JSONObject json = new JSONObject();
            for (String resultUri : resultUris) {
                json.append("items", new JSONObject().put("uri", resultUri));
            }
            return new ModelAndView(new JsonItemsView()).addObject("json", json);
        }


        Results r = resultsByUri.get(uri);
        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/workspace/jobs");
        if (!workDirsByUri.containsKey(scriptUri)) {
            return new ModelAndView(new ResourceNotFoundView());
        }

        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("log", uri + "/log");
        if (r.isSuccessful()) {
            json.put("status", "completed");
        } else {
            json.put("status", "running");
        }
        if (r.hasErrors()) {
            json.put("status", "error");
            json.put("message", r.getErrorMessage());
        }

        String baseWorkDir = workDirsByUri.get(scriptUri);
        for (File output : getOutputFiles(r.getOutputDirectory())) {
            JSONObject outputJson = new JSONObject();
            outputJson.put("uri", scriptUri + StringUtils.substringAfterLast(output.getPath(), baseWorkDir));
            outputJson.put("name", output.getName());
            json.append("items", outputJson);
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

    /*
     * Private Methods
     */

    private String getScriptUri(HttpServletRequest request) throws ResourceNotFoundException {
        String scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), "/workspace/jobs");
        if (!workDirsByUri.containsKey(scriptUri) && !scriptsByUri.containsKey(scriptUri)) {
            throw new ResourceNotFoundException(scriptUri);
        }
        return scriptUri;
    }

    private String getQueryString(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            if (!StringUtils.equalsIgnoreCase(paramName, "workspace")) {
                String[] paramValues = request.getParameterValues(paramName);
                for (String paramValue : paramValues) {
                    builder.append(paramName).append("=").append(paramValue).append("&");
                }
            }
        }

        log.info("getQueryString(" + request.getRequestURI() + "):[" + builder.toString() + "]");
        String queryString = builder.toString();
        if (queryString.endsWith("&")) {
            return StringUtils.substringBeforeLast(queryString, "&");
        }
        return queryString;
    }

    private Results executeRobot(String jobUri, String workspaceUri, String script, String jobDir) throws Exception {
        log.fine("executeRobot(" + jobUri + "," + workspaceUri + "," + script + "," + jobDir + ")");

        File outputDir = new File(jobDir + "/outputs");
        outputDir.mkdirs();

        File uploadsDir = new File(jobDir + "/uploads");
        uploadsDir.mkdirs();

        Process process = Runtime.getRuntime().exec(script, new String[0], outputDir);

        Results results = new Results(outputDir);

        OutputStream jobLog = new FileOutputStream(jobDir + "/job.log");
        Streamer stdout = new Streamer(process.getInputStream(), jobLog, true);
        Streamer stderr = new Streamer(process.getErrorStream(), jobLog, true);

        Workspace workspace = new Workspace(httpClientTemplate, jobUri, workspaceUri, new File(jobDir), uploadsDir);
        ExecutionRunnable execution = new ExecutionRunnable(process, workspace, results);
        execution.setCloseables(stdout, stderr, jobLog, workspace);

        new Thread(stdout).start();
        new Thread(stderr).start();
        new Thread(execution).start();

        return results;
    }

    private String[] getMatchingResultUris(String uri) {
        HashSet<String> matchingUris = new HashSet<String>();
        for (String resultUri : resultsByUri.keySet()) {
            if (resultUri.startsWith(uri) && !StringUtils.equalsIgnoreCase(resultUri, uri)) {
                matchingUris.add(resultUri);
            }
        }
        return matchingUris.toArray(new String[matchingUris.size()]);
    }
}
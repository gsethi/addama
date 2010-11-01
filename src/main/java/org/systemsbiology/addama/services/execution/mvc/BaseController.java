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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.registry.JsonConfigHandler;
import org.systemsbiology.addama.services.execution.dao.Job;
import org.systemsbiology.addama.services.execution.dao.JobsDao;
import org.systemsbiology.addama.services.execution.util.EmailJsonConfigHandler;
import org.systemsbiology.addama.services.execution.util.Mailer;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public abstract class BaseController implements InitializingBean {
    private static final Logger log = Logger.getLogger(BaseController.class.getName());

    protected final Map<String, String> workDirsByUri = new HashMap<String, String>();
    protected final Map<String, String> scriptsByUri = new HashMap<String, String>();
    protected final Map<String, String> logFilesByUri = new HashMap<String, String>();
    protected final Map<String, String> viewersByUri = new HashMap<String, String>();
    protected final HashMap<String, String> jobExecutionDirectoryByUri = new HashMap<String, String>();

    private JsonConfig jsonConfig;
    private EmailJsonConfigHandler emailJsonConfigHandler = new EmailJsonConfigHandler();
    protected JobsDao jobsDao;

    /*
    * Dependency Injection
    */

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void setEmailJsonConfigHandler(EmailJsonConfigHandler emailJsonConfigHandler) {
        this.emailJsonConfigHandler = emailJsonConfigHandler;
    }

    public void setJobsDao(JobsDao jobsDao) {
        this.jobsDao = jobsDao;
    }

    /*
     * InitializingBean
     */

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new MapJsonConfigHandler(workDirsByUri, "workDir"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(scriptsByUri, "script"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(logFilesByUri, "logFile"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(viewersByUri, "viewer"));
        jsonConfig.processConfiguration(new MapJsonConfigHandler(jobExecutionDirectoryByUri, "jobExecutionDirectory"));
        jsonConfig.processConfiguration(emailJsonConfigHandler);
    }

    /*
     * Protected Methods
     */

    protected Mailer getMailer(Job job) {
        if (this.emailJsonConfigHandler != null) {
            return this.emailJsonConfigHandler.getMailer(job.getScriptUri(), job.getLabel(), job.getEmail());
        }
        return null;
    }

    protected String getUserEmail(HttpServletRequest request) {
        String userUri = getUserUri(request);
        if (StringUtils.isEmpty(userUri)) {
            return null;
        }
        return StringUtils.substringAfterLast(userUri, "/addama/users/");
    }

    protected File[] getOutputFiles(File outputDirectory) throws JSONException {
        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, outputDirectory);
        return outputs.toArray(new File[outputs.size()]);
    }

    protected void scanOutputs(List<File> outputs, File outputDir) {
        if (outputDir.isFile()) {
            outputs.add(outputDir);
        }
        if (outputDir.isDirectory()) {
            for (File f : outputDir.listFiles()) {
                scanOutputs(outputs, f);
            }
        }
    }

    protected String getUserUri(HttpServletRequest request) {
        return request.getHeader("x-addama-registry-user");
    }

    protected String getScriptUri(HttpServletRequest request, String suffix) throws ResourceNotFoundException {
        String scriptUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
        if (!StringUtils.isEmpty(suffix)) {
            scriptUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), suffix);
        }

        if (!workDirsByUri.containsKey(scriptUri) && !scriptsByUri.containsKey(scriptUri)) {
            throw new ResourceNotFoundException(scriptUri);
        }
        log.info(scriptUri);
        return scriptUri;
    }

    protected Job getJob(HttpServletRequest request, String suffix) {
        String jobUri = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
        if (!StringUtils.isEmpty(suffix)) {
            jobUri = StringUtils.substringBetween(request.getRequestURI(), request.getContextPath(), suffix);
        }
        log.info(jobUri);
        return jobsDao.retrieve(jobUri);
    }

    protected String getJobExecutionDirectory(String scriptUri) {
        if (!jobExecutionDirectoryByUri.containsKey(scriptUri)) {
            return "outputs";
        }

        String jobExecutionDirectory = StringUtils.chomp(jobExecutionDirectoryByUri.get(scriptUri), "/");
        if (jobExecutionDirectory.startsWith("/")) {
            jobExecutionDirectory = StringUtils.substringAfter(jobExecutionDirectory, "/");
        }

        if (StringUtils.equalsIgnoreCase(jobExecutionDirectory, "null")) {
            return null;
        }

        return jobExecutionDirectory;
    }

    /*
    * Private Classes
    */

    private class MapJsonConfigHandler implements JsonConfigHandler {
        private final Map<String, String> propertiesByUri;
        private final String propertyName;

        public MapJsonConfigHandler(Map<String, String> map, String name) {
            this.propertiesByUri = map;
            this.propertyName = name;
        }

        public void handle(JSONObject configuration) throws Exception {
            if (configuration.has("locals")) {
                JSONArray locals = configuration.getJSONArray("locals");
                for (int i = 0; i < locals.length(); i++) {
                    JSONObject local = locals.getJSONObject(i);
                    if (local.has(propertyName)) {
                        propertiesByUri.put(local.getString("uri"), local.getString(propertyName));
                    }
                }
            }
        }

    }
}
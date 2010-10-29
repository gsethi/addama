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
package org.systemsbiology.addama.services.execution.dao;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hrovira
 */
public class Job {
    private final String jobUri;
    private final String userUri;
    private final String scriptUri;
    private final String jobDirectory;

    private JobStatus jobStatus = JobStatus.pending;
    private String label;
    private String executionDirectory;
    private String errorMessage;

    /*
     * Constructor
     */

    public Job(String jobUri, String jobDirectory, String userUri) {
        this.jobUri = jobUri;
        this.userUri = userUri;
        this.jobDirectory = jobDirectory;
        this.scriptUri = StringUtils.substringBeforeLast(jobUri, "/jobs");
    }

    /*
     * Required
     */

    public String getJobUri() {
        return jobUri;
    }

    public String getUserUri() {
        return userUri;
    }

    public String getJobDirectory() {
        return jobDirectory;
    }

    /*
     * Optional
     */

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getExecutionDirectory() {
        return executionDirectory;
    }

    public void setExecutionDirectory(String executionDirectory) {
        this.executionDirectory = executionDirectory;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /*
    * Derived
    */

    public String getLogPath() {
        return jobDirectory + "/job.log";
    }

    public String getOutputDirectoryPath() {
        return jobDirectory + "/outputs";
    }

    public String getScriptUri() {
        return StringUtils.substringBeforeLast(jobUri, "/jobs");
    }

    public String getExecuteAtPath() {
        if (!StringUtils.isEmpty(executionDirectory)) {
            return jobDirectory + "/" + executionDirectory;
        }
        return getOutputDirectoryPath();
    }

    /*
    * Public Methods
    */

    public String getEmail() {
        return StringUtils.substringAfterLast(userUri, "/addama/users/");
    }

    public JSONObject getJsonDetail() throws JSONException {
        JSONObject json = getJsonSummary();
        json.put("message", errorMessage);

        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, new File(getOutputDirectoryPath()));

        for (File output : outputs) {
            JSONObject outputJson = new JSONObject();
            outputJson.put("uri", scriptUri + StringUtils.substringAfterLast(output.getPath(), jobDirectory));
            outputJson.put("name", output.getName());
            json.append("outputs", outputJson);
        }

        return json;
    }

    public JSONObject getJsonSummary() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", jobUri);
        json.put("label", label);
        json.put("log", getLogPath());
        json.put("script", scriptUri);
        json.put("status", jobStatus);
        return json;
    }

    /*
     * Private Methods
     */

    private void scanOutputs(List<File> outputs, File outputDir) {
        if (outputDir.isFile()) {
            outputs.add(outputDir);
        }
        if (outputDir.isDirectory()) {
            for (File f : outputDir.listFiles()) {
                scanOutputs(outputs, f);
            }
        }
    }
}

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author hrovira
 */
public class Job {
    private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final String jobUri;
    private final String userUri;
    private final String scriptUri;
    private final String jobDirectory;
    private final JSONObject inputs;

    private JobStatus jobStatus = JobStatus.pending;
    private String label;
    private String executionDirectory;
    private String errorMessage;
    private Date createdAt;
    private Date modifiedAt;

    /*
     * Constructor
     */

    public Job(String jobUri, String scriptUri, String userUri, String jobDirectory, JSONObject inputs) {
        this.jobUri = jobUri;
        this.scriptUri = scriptUri;
        this.userUri = userUri;
        this.jobDirectory = jobDirectory;
        this.inputs = inputs;
        this.createdAt = new Date();
        this.modifiedAt = new Date();
    }

    /*
     * Required
     */

    public String getJobUri() {
        return jobUri;
    }

    public String getScriptUri() {
        return scriptUri;
    }

    public String getUserUri() {
        return userUri;
    }

    public String getJobDirectory() {
        return jobDirectory;
    }

    public JSONObject getInputs() {
        return inputs;
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
    * Dates
    */
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
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

    public String getExecuteAtPath() {
        if (!StringUtils.isEmpty(executionDirectory)) {
            return jobDirectory + "/" + executionDirectory;
        }
        return getOutputDirectoryPath();
    }

    public String getQueryString() throws JSONException {
        StringBuilder builder = new StringBuilder();
        Iterator itr = inputs.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            boolean isNotLast = itr.hasNext();

            JSONArray values = inputs.optJSONArray(key);
            if (values != null) {
                for (int i = 0; i < values.length(); i++) {
                    builder.append(key);
                    builder.append("=");
                    builder.append(values.getString(i));
                    if (isNotLast) {
                        builder.append("&");
                    }
                }
            } else {
                builder.append(key);
                builder.append("=");
                builder.append(inputs.getString(key));
                if (isNotLast) {
                    builder.append("&");
                }
            }
        }
        return builder.toString();
    }

    public String getFormattedCreatedAt() {
        return dateFormat.format(createdAt);
    }

    public String getFormattedModifiedAt() {
        return dateFormat.format(modifiedAt);
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
        json.put("inputs", inputs);

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
        json.put("log", jobUri + "/log");
        json.put("script", scriptUri);
        json.put("status", jobStatus);
        json.put("owner", userUri);
        json.put("created", getFormattedCreatedAt());
        json.put("lastModified", getFormattedModifiedAt());
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

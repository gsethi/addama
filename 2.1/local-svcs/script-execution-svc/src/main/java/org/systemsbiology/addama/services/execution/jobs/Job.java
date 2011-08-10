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
package org.systemsbiology.addama.services.execution.jobs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.services.execution.jobs.JobStatus.pending;

/**
 * @author hrovira
 */
public class Job {
    private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final String jobUri;
    private final String userUri;
    private final String scriptUri;
    private final String jobDirectory;
    private String scriptPath;

    private JobStatus jobStatus = pending;
    private String label;
    private String executionDirectory;
    private String errorMessage;
    private Integer returnCode = 0;
    private Date createdAt;
    private Date modifiedAt;
    private String scriptArgs;
    private String channelUri;

    /*
     * Constructor
     */

    public Job(String jobUri, String scriptUri, String userUri, String jobDirectory, String scriptPath) {
        this.jobUri = jobUri;
        this.scriptUri = scriptUri;
        this.userUri = userUri;
        this.jobDirectory = jobDirectory;
        this.createdAt = new Date();
        this.modifiedAt = new Date();
        this.scriptPath = scriptPath;
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

    public String getScriptPath() {
        return scriptPath;
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

    public void setExecutionDirectoryFromConfiguration(String configuredExecDir) {
        this.executionDirectory = chomp(jobDirectory, "/") + subExecutionDirectory(configuredExecDir);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public String getScriptArgs() {
        return scriptArgs;
    }

    public void setScriptArgs(String scriptArgs) {
        this.scriptArgs = scriptArgs;
    }

    public String getChannelUri() {
        return channelUri;
    }

    public void setChannelUri(String channelUri) {
        this.channelUri = channelUri;
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
        return substringAfterLast(userUri, "/addama/users/");
    }

    public JSONObject getJsonDetail() throws JSONException {
        JSONObject json = getJsonSummary();
        json.put("inputs", scriptArgs);

        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, new File(getOutputDirectoryPath()));

        for (File output : outputs) {
            JSONObject outputJson = new JSONObject();
            String fileUri = jobUri + substringAfterLast(output.getPath(), jobDirectory);
            outputJson.put("uri", replace(fileUri, "outputs/", "outputs/_afdl/"));
            outputJson.put("query", fileUri + "/query");
            outputJson.put("name", output.getName());
            json.append("items", outputJson);
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
        json.put("returnCode", returnCode);
        json.put("message", errorMessage);
        json.put("created", getFormattedCreatedAt());
        json.put("lastModified", getFormattedModifiedAt());
        json.put("durationInSeconds", getDurationInSecs());
        return json;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("\t").append("   job=").append(this.jobUri).append("\n");
        builder.append("\t").append("script=").append(this.scriptUri).append("\n");
        builder.append("\t").append("  user=").append(this.userUri).append("\n");
        builder.append("\t").append("jobdir=").append(this.jobDirectory).append("\n");
        builder.append("\t").append("execat=").append(this.executionDirectory).append("\n");
        return builder.toString();
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

    private String subExecutionDirectory(String configuredExecDir) {
        if (isEmpty(configuredExecDir)) {
            return "/outputs";
        }

        if (equalsIgnoreCase(configuredExecDir, "null")) {
            return "";
        }

        String sub = chomp(configuredExecDir, "/");
        if (sub.startsWith("/")) {
            return substringAfter(sub, "/");
        }
        return sub;
    }

    private double getDurationInSecs() {
        if (createdAt == null) return 0;

        Date modified = new Date();
        if (modifiedAt != null) {
            modified = modifiedAt;
        }

        long duration = modified.getTime() - createdAt.getTime();
        if (duration <= 0) {
            return 0;
        }
        return duration / 1000;
    }
}

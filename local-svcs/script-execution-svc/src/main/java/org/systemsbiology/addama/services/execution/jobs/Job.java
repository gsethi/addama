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

    private final String jobId;
    private final String toolId;
    private final String toolUri;
    private final String owner;
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

    /*
     * Constructor
     */

    public Job(String jobId, String toolId, String toolUri, String owner, String jobDirectory, String scriptPath) {
        this.jobId = jobId;
        this.toolId = toolId;
        this.toolUri = toolUri;
        this.owner = owner;
        this.jobDirectory = jobDirectory;
        this.createdAt = new Date();
        this.modifiedAt = new Date();
        this.scriptPath = scriptPath;
    }

    /*
     * Required
     */

    public String getJobId() {
        return jobId;
    }

    public String getToolId() {
        return toolId;
    }

    public String getToolUri() {
        return toolUri;
    }

    public String getOwner() {
        return owner;
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

    public JSONObject getJsonDetail() throws JSONException {
        JSONObject json = getJsonSummary();
        json.put("inputs", scriptArgs);

        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, new File(getOutputDirectoryPath()));

        String uri = chomp(toolUri, "/") + "/jobs/" + jobId;
        for (File output : outputs) {
            JSONObject outputJson = new JSONObject();
            String fileUri = uri + "/" + substringAfterLast(output.getPath(), jobDirectory);
            outputJson.put("uri", replace(fileUri, "outputs/", "outputs/_afdl/"));
            outputJson.put("query", fileUri + "/query");
            outputJson.put("name", output.getName());
            json.append("items", outputJson);
        }

        return json;
    }

    public JSONObject getJsonOutputs() throws JSONException {
        JSONObject json = new JSONObject();

        List<File> outputs = new ArrayList<File>();
        scanOutputs(outputs, new File(getOutputDirectoryPath()));

        String uri = chomp(toolUri, "/") + "/jobs/" + jobId;
        for (File output : outputs) {
            JSONObject outputJson = new JSONObject();
            String fileUri = uri + "/" + substringAfterLast(output.getPath(), jobDirectory);
            outputJson.put("uri", replace(fileUri, "outputs/", "outputs/_afdl/"));
            outputJson.put("query", fileUri + "/query");
            outputJson.put("name", output.getName());
            json.append("items", outputJson);
        }

        return json;
    }

    public JSONObject getJsonSummary() throws JSONException {
        String uri = chomp(toolUri, "/") + "/jobs/" + jobId;
        JSONObject json = new JSONObject();
        json.put("uri", uri);
        json.put("label", label);
        json.put("log", uri + "/log");
        json.put("tool", toolUri);
        json.put("status", jobStatus);
        json.put("owner", owner);
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
        builder.append("\t").append("   job=").append(this.jobId).append("\n");
        builder.append("\t").append("  tool=").append(this.toolId).append("\n");
        builder.append("\t").append("  user=").append(this.owner).append("\n");
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

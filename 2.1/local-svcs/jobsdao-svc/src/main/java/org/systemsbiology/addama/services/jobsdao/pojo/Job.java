package org.systemsbiology.addama.services.jobsdao.pojo;

import static org.apache.commons.lang.StringUtils.*;
import org.json.JSONException;
import org.json.JSONObject;
import static org.systemsbiology.addama.services.jobsdao.pojo.JobStatus.pending;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author: hrovira
 */
public class Job {
    private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final String jobUri;
    private final String userUri;

    private JobStatus jobStatus = pending;
    private String label;
    private String errorMessage;
    private Integer returnCode = 0;
    private Date createdAt;
    private Date modifiedAt;

    /*
     * Constructor
     */

    public Job(String jobUri, String userUri) {
        this.jobUri = jobUri;
        this.userUri = userUri;
        this.createdAt = new Date();
        this.modifiedAt = new Date();
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
//        json.put("inputs", scriptArgs);

//        List<File> outputs = new ArrayList<File>();
//        scanOutputs(outputs, new File(getOutputDirectoryPath()));
//        for (File output : outputs) {
//            JSONObject outputJson = new JSONObject();
//            String fileUri = jobUri + substringAfterLast(output.getPath(), jobDirectory);
//            outputJson.put("uri", replace(fileUri, "outputs/", "outputs/_afdl/"));
//            outputJson.put("name", output.getName());
//            json.append("items", outputJson);
//        }

        return json;
    }

    public JSONObject getJsonSummary() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uri", jobUri);
        json.put("label", label);
        json.put("log", jobUri + "/log");
        json.put("status", jobStatus);
        json.put("owner", userUri);
        json.put("returnCode", returnCode);
        json.put("message", errorMessage);
        json.put("created", getFormattedCreatedAt());
        json.put("lastModified", getFormattedModifiedAt());
        json.put("durationInSeconds", getDurationInSecs());
        return json;
    }

    @Override
    public String toString() {
        return "[" + jobUri + "," + userUri + "]";
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

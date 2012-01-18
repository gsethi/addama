package org.systemsbiology.addama.services.execution.jobs;

import java.util.HashMap;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class ReturnCodes {
    private final Integer successCode;
    private final HashMap<Integer, String> reasonsByReturn = new HashMap<Integer, String>();
    private String unknownReason = "Unknown Failure";

    public ReturnCodes(Integer success) {
        this.successCode = success;
    }

    public void addReasonByErrorCode(Integer code, String reason) {
        this.reasonsByReturn.put(code, reason);
    }

    public Integer getSuccessCode() {
        return successCode;
    }

    public void setUnknownReason(String unknown) {
        if (!isEmpty(unknown)) {
            this.unknownReason = unknown;
        }
    }

    public String getReason(Integer code) {
        if (this.reasonsByReturn.containsKey(code)) {
            return this.reasonsByReturn.get(code);
        }
        return unknownReason;
    }
}

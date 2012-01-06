package org.systemsbiology.addama.experimental.asynch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class Response implements Serializable {
    private final String uri;
    private final String url;

    private Status status = Status.pending;
    private String contentType;
    private byte[] content;

    public Response(String uri, String url) {
        this.uri = uri;
        this.url = url;
    }

    public String getUri() {
        return this.uri;
    }

    public String getUrl() {
        return this.url;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public String toString() {
        String r = "[" + uri + "," + url + "," + status;
        if (content != null) {
            r += "," + contentType + "," + content.length;
        }
        return r + "]";
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("asynch", uri);
        json.put("status", status);
        if (content != null) {
            json.put("content-length", content.length);
        }
        if (!isEmpty(contentType)) {
            json.put("content-type", contentType);
        }
        return json;
    }
}

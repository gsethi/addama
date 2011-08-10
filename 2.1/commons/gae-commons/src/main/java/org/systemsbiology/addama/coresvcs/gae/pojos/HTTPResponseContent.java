package org.systemsbiology.addama.coresvcs.gae.pojos;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;

import java.io.Serializable;
import java.util.List;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

/**
 * @author hrovira
 */
public class HTTPResponseContent implements Serializable {
    private final byte[] bytes;
    private boolean html;
    private String contentType;

    /*
     * Constructors
     */

    public HTTPResponseContent(HTTPResponse resp) {
        this.bytes = resp.getContent();
        this.evaluateContentType(resp);
    }

    public HTTPResponseContent(HTTPResponse resp, byte[] content) {
        this.bytes = content;
        this.evaluateContentType(resp);
    }

    /*
     * Getters
     */

    public byte[] getBytes() {
        return this.bytes;
    }

    public boolean isHtml() {
        return this.html;
    }

    public String getContentType() {
        return contentType;
    }

    /*
     * Private Methods
     */

    private void evaluateContentType(HTTPResponse resp) {
        if (resp != null) {
            List<HTTPHeader> headers = resp.getHeaders();
            if (headers != null) {
                for (HTTPHeader header : headers) {
                    if (equalsIgnoreCase(header.getName(), "content-type")) {
                        this.contentType = header.getValue();
                        this.html = this.contentType.startsWith("text/html");
                        return;
                    }
                }
            }
        }
    }
}

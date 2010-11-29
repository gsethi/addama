package org.systemsbiology.addama.coresvcs.gae.filters.callbacks;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * @author hrovira
 */
public class HTTPResponseContent implements Serializable {
    private final byte[] bytes;
    private boolean html;

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

    /*
     * Private Methods
     */

    private void evaluateContentType(HTTPResponse resp) {
        if (resp != null) {
            List<HTTPHeader> headers = resp.getHeaders();
            if (headers != null) {
                for (HTTPHeader header : headers) {
                    if (StringUtils.equalsIgnoreCase(header.getName(), "content-type")) {
                        this.html = header.getValue().startsWith("text/html");
                        return;
                    }
                }
            }
        }
    }
}

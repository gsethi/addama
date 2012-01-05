package org.systemsbiology.addama.coresvcs.gae.pojos;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.users.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class HTTPResponseContent implements Serializable {
    private final byte[] bytes;
    private String contentType = "text/html";

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
        return this.contentType.startsWith("text/html");
    }

    public String getContentType() {
        return this.contentType;
    }

    /*
     * Public Static Methods
     */
    public static void serveContent(HTTPResponseContent content, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (content == null) {
            response.setStatus(SC_NOT_FOUND);
            return;
        }

        UserService userService = getUserService();
        // enforce login if required on HTML pages... not JS, CSS or Images
        if (content.isHtml() && !userService.isUserLoggedIn()) {
            response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
            return;
        }

        applyContentType(content, request, response);
        response.getOutputStream().write(content.getBytes());
    }

    public static void applyContentType(HTTPResponseContent content, HttpServletRequest request, HttpServletResponse response) {
        String mimeType = request.getSession().getServletContext().getMimeType(request.getRequestURI());
        if (!isEmpty(mimeType)) {
            response.setContentType(mimeType);
            return;
        }

        // output external content
        if (!isEmpty(content.getContentType())) {
            response.setContentType(content.getContentType());
        }
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
                        return;
                    }
                }
            }
        }
    }
}

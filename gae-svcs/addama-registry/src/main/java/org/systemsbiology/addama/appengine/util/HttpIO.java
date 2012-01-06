package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import org.systemsbiology.addama.appengine.pojos.MapReduceTooLargeHTTPResponse;
import org.systemsbiology.addama.appengine.pojos.HTTPResponseContent;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.contains;

/**
 * @author hrovira
 */
public class HttpIO {

    public static HTTPResponseContent mapReduce(HTTPRequest request) throws Exception {
        MapReduceTooLargeHTTPResponse mapReduce = new MapReduceTooLargeHTTPResponse();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        HTTPResponse resp = mapReduce.fetch(request, outputStream);
        if (outputStream.size() > 0) {
            return new HTTPResponseContent(resp, outputStream.toByteArray());
        }

        if (resp.getResponseCode() == SC_OK) {
            return new HTTPResponseContent(resp);
        }

        return null;
    }

    public static boolean shouldSendRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session != null) {
            ServletContext servletContext = session.getServletContext();
            if (servletContext != null) {
                String mimeType = servletContext.getMimeType(request.getRequestURI());
                return contains(mimeType, "text/html");
            }
        }
        return false;
    }
}

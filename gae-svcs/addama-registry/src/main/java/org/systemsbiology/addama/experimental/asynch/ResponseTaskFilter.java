package org.systemsbiology.addama.experimental.asynch;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskOptions;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.appengine.pojos.RegistryMapping;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static com.google.appengine.api.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.util.ApiKeys.getUserEmailFromApiKey;
import static org.systemsbiology.addama.experimental.asynch.Status.retrieved;
import static org.systemsbiology.addama.experimental.asynch.Status.running;
import static org.systemsbiology.addama.experimental.asynch.TaskingController.responses;

/**
 * @author hrovira
 */
public class ResponseTaskFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(ResponseTaskFilter.class.getName());

    public static final String ASYNCH_MODE = "x-addama-asynch-mode";

    private final Queue channelQueue = getDefaultQueue();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith("/addama/asynch/tasks/")) {
            try {
                log.info("not-asynch");
                filterChain.doFilter(request, response);
            } catch (AsynchRequestException e) {
                log.info("process-asynch:" + e.getMessage());

                String queryString = request.getQueryString();
                if (isEmpty(queryString)) {
                    response.sendRedirect("/addama/asynch" + requestUri);
                } else {
                    response.sendRedirect("/addama/asynch" + requestUri + "?" + queryString);
                }
            }

            return;
        }

        handleAsynch(request, response, filterChain);
    }

    public static void checkAsynch(HttpServletRequest request, RegistryMapping mapping) {
        if (!equalsIgnoreCase(request.getMethod(), "get")) {
            log.info("not get");
            return;
        }

        if (!mapping.isHandleAsynch()) {
            log.info("not asynch mapping");
            return;
        }

        if (isInAsynchMode(request)) {
            log.info("in asynch mode: proceed normal");
            return;
        }

        throw new AsynchRequestException(mapping.getUri());
    }

    /*
    * Private Methods
    */

    private void handleAsynch(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String responseUri = replace(request.getRequestURI(), "/addama/asynch/tasks/", "/addama/asynch/responses/");
        log.info(responseUri);

        Response r = (Response) responses.get(responseUri);
        if (r == null) {
            // todo: determine if any other cases may cause null
            // this will cause the task to retry, possibly resolving race condition with memcache
            response.setStatus(SC_NOT_FOUND);
            return;
        }

        // process request
        r.setStatus(running);
        responses.put(r.getUri(), r);

        log.info("running:" + r.getUrl());

        // TODO: Fix this later... dealing with servlet-api issues
        HttpServletRequest maskUriRequest = null;
//        maskUriRequest.setAttribute(ASYNCH_MODE, true);

        CaptureOutputResponse captureResponse = new CaptureOutputResponse(response);

        filterChain.doFilter(maskUriRequest, captureResponse);

        if (captureResponse.getStatus() == SC_OK) {
            // capture response contents into memcache
            r.setStatus(retrieved);
            r.setContentType(captureResponse.getContentType());
            r.setContent(captureResponse.getContent());
            responses.put(r.getUri(), r);
            log.info("retrieved:" + r);

            // publish on user channel
            publishOnUserChannel(r, request.getHeader("x-addama-apikey"));
            return;
        }

        response.setStatus(captureResponse.getStatus());
    }

    private void publishOnUserChannel(Response r, String apiKey) throws IOException {
        try {
            String userEmail = getUserEmailFromApiKey(apiKey);
            String channelUri = "/addama/channels/" + userEmail;
            log.info("channelUri=" + channelUri);

            TaskOptions task = withUrl(channelUri);
            task.header("x-addama-apikey", apiKey);
            task.param("event", r.toJSON().toString());
            channelQueue.add(task);
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }

    private static boolean isInAsynchMode(HttpServletRequest request) {
        try {
            Object flag = request.getAttribute(ASYNCH_MODE);
            if (flag != null) {
                return (Boolean) flag;
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return false;
    }

}

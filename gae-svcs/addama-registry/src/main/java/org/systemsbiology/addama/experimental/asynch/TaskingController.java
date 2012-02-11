package org.systemsbiology.addama.experimental.asynch;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskOptions;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.appengine.memcache.MemcacheLoaderCallback;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static com.google.appengine.api.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.util.ApiKeys.getUserApiKey;
import static org.systemsbiology.addama.appengine.memcache.MemcacheServiceTemplate.loadIfNotExisting;
import static org.systemsbiology.addama.experimental.asynch.Status.pending;

/**
 * @author hrovira
 */
public class TaskingController extends AbstractController implements MemcacheLoaderCallback {
    private static final Logger log = Logger.getLogger(TaskingController.class.getName());

    protected static final MemcacheService responseUriByUrl = getMemcacheService("asynch-response-uris");
    protected static final MemcacheService responses = getMemcacheService("asynch-responses");

    private final Queue queue = getDefaultQueue();

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = urlForTask(request);
        log.info(url);

        // generate transient uri to provide client handle on asynch request
        String uri = (String) loadIfNotExisting(responseUriByUrl, url, this, byDeltaSeconds(300));
        Response r = (Response) responses.get(uri);
        log.info(r.toString());

        if (r.getStatus().equals(pending)) {
            TaskOptions task = withUrl(replace(uri, "/addama/asynch/responses/", "/addama/asynch/tasks/"));
            task.header("x-addama-apikey", userApiKey(request));

            Enumeration keys = request.getParameterNames();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String[] values = request.getParameterValues(key);
                if (values != null) {
                    for (String value : values) {
                        task.param(key, value);
                    }
                }
            }

            queue.add(task);
        }

        // redirect client to response location
        response.sendRedirect(uri);
        return null;
    }

    public Serializable getCacheableObject(String url) throws Exception {
        if (contains(url, "?")) {
            url = substringBefore(url, "?");
        }

        String uri = "/addama/asynch/responses/" + UUID.randomUUID();
        responses.put(uri, new Response(uri, url));
        return uri;
    }

    /*
     * Private Methods
     */

    private String urlForTask(HttpServletRequest request) {
        String targetUrl = substringAfter(request.getRequestURI(), "/addama/asynch");
        String queryString = request.getQueryString();
        if (isEmpty(queryString)) {
            return targetUrl;
        }
        return targetUrl + "?" + request.getQueryString();
    }

    private String userApiKey(HttpServletRequest request) throws ForbiddenAccessException {
        String apikey = request.getHeader("x-addama-apikey");
        if (!isEmpty(apikey)) {
            return apikey;
        }

        return getUserApiKey().toString();
    }

}

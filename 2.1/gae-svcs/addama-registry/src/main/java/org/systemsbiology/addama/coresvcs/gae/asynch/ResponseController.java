package org.systemsbiology.addama.coresvcs.gae.asynch;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;
import static org.systemsbiology.addama.coresvcs.gae.asynch.Status.retrieved;
import static org.systemsbiology.addama.coresvcs.gae.asynch.TaskingController.responses;

/**
 * @author hrovira
 */
public class ResponseController extends AbstractController {
    private static final Logger log = Logger.getLogger(ResponseController.class.getName());

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.fine(requestUri);

        // request has been expired?
        if (!responses.contains(requestUri)) {
            throw new ResourceNotFoundException(requestUri);
        }

        Status status = processResponse(requestUri, response);
        if (status.equals(retrieved)) {
            // retrieve data completed
            return null;
        }

        // todo: sleep time from registry configuration or mapping configuration
        // block here so we don't have to deal with timing issues in Javascript on the client
        sleep(200);

        // todo: max number of times from registry configuration or mapping configuration
        if (numberOfTries(request) < 5) {
            // try the cache again...
            return handleRequestInternal(request, response);
        }

        log.info("redirect to: " + requestUri);
        // give up... set client to try again
        response.sendRedirect(requestUri);

        // todo: determine if we should send JSON with information on redirect
        return null;
    }

    /*
     * Private Methods
     */

    private Status processResponse(String requestUri, HttpServletResponse response) throws IOException {
        Response r = (Response) responses.get(requestUri);
        log.info(r.toString());

        Status status = r.getStatus();
        if (status.equals(retrieved)) {
            // write out the response from the cache
            response.setContentType(r.getContentType());
            response.getOutputStream().write(r.getContent());
        }
        return status;
    }

    private static int numberOfTries(HttpServletRequest request) {
        try {
            Integer numberOfTries = (Integer) request.getAttribute("x-addama-asynch-numberOfTries");
            request.setAttribute("x-addama-asynch-numberOfTries", numberOfTries + 1);
            return numberOfTries;
        } catch (Exception e) {
            log.warning(e.getMessage());
            // start counter
            request.setAttribute("x-addama-asynch-numberOfTries", 0);
            return 0;
        }
    }
}

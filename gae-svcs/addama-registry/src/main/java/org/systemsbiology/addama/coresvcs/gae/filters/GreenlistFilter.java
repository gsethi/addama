package org.systemsbiology.addama.coresvcs.gae.filters;

import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserUri;
import static org.systemsbiology.addama.appengine.util.Greenlist.isUserInGreenlist;

/**
 * @author aeakin
 */
public class GreenlistFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(GreenlistFilter.class.getName());

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (equalsIgnoreCase("get", request.getMethod()) && request.getRequestURI().startsWith("/addama/pubget")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String userUri = getLoggedInUserUri(request);
        Boolean isAdmin = (Boolean) servletRequest.getAttribute("isAdmin");

        // CODE_REVIEW: Is it possible that isAdmin flag could be null?  Will auto-boxing take care of this?
        if (isAdmin) {
            log.info("user is admin");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // CODE_REVIEW: What happens if there is no white list for the URI?
        if (isUserInGreenlist(userUri, request.getRequestURI())) {
            log.fine("user in greenlist");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        //how does this redirect work in the case of apikeys?
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.sendRedirect("/html/bounced.html");
    }
}

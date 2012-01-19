package org.systemsbiology.addama.appengine.filters;

import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.systemsbiology.addama.appengine.util.Greenlist.isGreenlistActive;
import static org.systemsbiology.addama.appengine.util.Greenlist.isGreenlisted;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserEmail;
import static org.systemsbiology.addama.appengine.util.Users.isAdministrator;

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

        if (isAdministrator(request)) {
            log.info("user is admin");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!isGreenlistActive()) {
            log.info("no greenlist active");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String userEmail = getLoggedInUserEmail(request);
        if (isGreenlisted(userEmail)) {
            log.info("user in greenlist");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        //how does this redirect work in the case of apikeys?
        response.setStatus(SC_UNAUTHORIZED);
        // response.sendRedirect("/html/bounced.html");
    }
}

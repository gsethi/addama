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
import static org.apache.commons.lang.StringUtils.*;
import static org.systemsbiology.addama.appengine.util.HttpIO.shouldSendRedirect;
import static org.systemsbiology.addama.appengine.util.Memberships.*;
import static org.systemsbiology.addama.appengine.util.Memberships.Membership.guest;
import static org.systemsbiology.addama.appengine.util.Memberships.Membership.member;
import static org.systemsbiology.addama.appengine.util.Users.getLoggedInUserEmail;
import static org.systemsbiology.addama.appengine.util.Users.isAdministrator;
import static org.systemsbiology.addama.commons.gae.Appspot.APPSPOT_URL;

/**
 * @author hrovira
 */
public class MembershipFilter extends GenericFilterBean {
    private static final String PAGE = "/html/memberships/apply.html";

    private static final Logger log = Logger.getLogger(MembershipFilter.class.getName());

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if (isAdministrator(request)) {
            log.info("allowed: is administrator");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        Membership lowestRequired = equalsIgnoreCase(request.getMethod(), "get") ? guest : member;
        DomainMember candidate = new DomainMember(getLoggedInUserEmail(request), lowestRequired);

        if (!isAllowedInDomain(candidate)) {
            handleUnauthorized(request, servletResponse, null);
            return;
        }

        ModeratedItem moderatedItem = getModerated(request.getRequestURI());
        if (moderatedItem == null) {
            log.info("allowed: not moderated");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (isAllowedAs(moderatedItem, candidate)) {
            log.info("allowed: " + candidate);
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        handleUnauthorized(request, servletResponse, moderatedItem);
    }

    /*
     * Private Methods
     */

    private void handleUnauthorized(HttpServletRequest request, ServletResponse servletResponse, ModeratedItem moderatedItem) throws IOException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String page = PAGE;
        if (moderatedItem != null) {
            page += "?uri=" + moderatedItem.getUri();
        }

        if (shouldSendRedirect(request)) {
            response.sendRedirect(page);
        } else {
            response.sendError(SC_UNAUTHORIZED, "Request access here: " + APPSPOT_URL + page);
        }
    }

    private ModeratedItem getModerated(String requestUri) {
        if (isEmpty(requestUri)) {
            return null;
        }

        ModeratedItem item = getModeratedItem(requestUri);
        if (item != null) {
            return item;
        }

        return getModerated(substringAfterLast(requestUri, "/"));
    }

}

package org.systemsbiology.addama.coresvcs.gae.filters;

import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.coresvcs.gae.services.WhiteLists;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author aeakin
 */
public class WhiteListFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(WhiteListFilter.class.getName());

    private WhiteLists whiteLists;

    public void setWhiteLists(WhiteLists whiteLists) {
        this.whiteLists = whiteLists;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // CODE_REVIEW: Java Logging will already put the name of the class and the method in the logs before the message
        log.info("doFilterWhiteList");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String userUri = (String) servletRequest.getAttribute("userUri");
        Boolean isAdmin = (Boolean) servletRequest.getAttribute("isAdmin");

        // CODE_REVIEW: Is it possible that isAdmin flag could be null?  Will auto-boxing take care of this?
        if (isAdmin) {
            log.info("doFilterWhiteList: user is admin");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // CODE_REVIEW: What happens if there is no white list for the URI?
        if (whiteLists.isUserInWhiteList(userUri, request.getRequestURI())) {
            log.fine("doFilterWhiteList: user in whitelist");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        //how does this redirect work in the case of apikeys?
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.sendRedirect("/requestaccess.html?uri=" + request.getRequestURI());
    }
}

package org.systemsbiology.addama.appengine.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class AllowThroughFilter implements Filter {
    private static final Logger log = Logger.getLogger(AllowThroughFilter.class.getName());

    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        log.info("skipping rest of filter chain [" + ((HttpServletRequest) req).getRequestURI() + "]");
    }

    public void init(FilterConfig config) throws ServletException {
    }

}

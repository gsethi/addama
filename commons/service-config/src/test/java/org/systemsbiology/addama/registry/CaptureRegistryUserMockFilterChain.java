package org.systemsbiology.addama.registry;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author hrovira
 */
public class CaptureRegistryUserMockFilterChain implements FilterChain {
    private String user;

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        this.user = request.getHeader("x-addama-registry-user");
    }

    public String getUser() {
        return user;
    }
}

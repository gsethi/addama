package org.systemsbiology.addama.appengine.filters;

import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import static com.google.appengine.api.channel.ChannelServiceFactory.getChannelService;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.chomp;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.systemsbiology.addama.appengine.util.Channels.dropChannel;

/**
 * @author hrovira
 */
public class ChannelPresenceFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(ChannelPresenceFilter.class.getName());

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String method = request.getMethod();
        if (equalsIgnoreCase(method, "POST")) {
            String uri = chomp(request.getRequestURI(), "/");
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            if (equalsIgnoreCase(uri, "/_ah/channel/disconnected")) {
                outputHeaders(request);
                dropChannel(getChannelService().parsePresence(request));
                response.setStatus(SC_OK);
                return;
            }

            if (equalsIgnoreCase(uri, "/_ah/channel/connected")) {
                outputHeaders(request);
                response.setStatus(SC_OK);
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void outputHeaders(HttpServletRequest request) {
        log.info("::" + request.getParameterMap());

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            log.info(headerName + ":" + request.getHeader(headerName));
        }
    }
}

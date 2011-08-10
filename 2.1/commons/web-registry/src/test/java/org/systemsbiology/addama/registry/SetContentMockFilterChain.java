package org.systemsbiology.addama.registry;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author hrovira
 */
public class SetContentMockFilterChain implements FilterChain {
    private final Integer contentLength;

    public SetContentMockFilterChain(Integer contentLength) {
        this.contentLength = contentLength;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        OutputStream outputStream = servletResponse.getOutputStream();
        for (int i = 0; i < contentLength; i++) {
            outputStream.write(1);
        }
    }
}

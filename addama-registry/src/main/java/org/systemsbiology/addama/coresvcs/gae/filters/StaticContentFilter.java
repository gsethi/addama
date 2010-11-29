/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.coresvcs.gae.filters;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheLoaderCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcacheServiceTemplate;
import org.systemsbiology.addama.coresvcs.gae.filters.callbacks.StaticContentMemcacheLoaderCallback;
import org.systemsbiology.addama.coresvcs.gae.filters.callbacks.UiBaseMemcacheLoaderCallback;
import org.systemsbiology.addama.coresvcs.gae.services.Registry;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class StaticContentFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(StaticContentFilter.class.getName());

    private Registry registry;
    private MemcacheServiceTemplate memcacheServiceTemplate;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setMemcacheServiceTemplate(MemcacheServiceTemplate template) {
        this.memcacheServiceTemplate = template;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            log.info("doFilter");

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String method = request.getMethod();
            if (!method.equalsIgnoreCase("GET")) {
                log.info("doFilter:method=" + method);
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            String requestUri = request.getRequestURI();
            if (requestUri.endsWith(".html")) {
                UserService userService = UserServiceFactory.getUserService();
                if (!userService.isUserLoggedIn()) {
                    HttpServletResponse response = (HttpServletResponse) servletResponse;
                    response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
                    return;
                }
            }

            MemcacheLoaderCallback callback = new UiBaseMemcacheLoaderCallback();
            if (!requestUri.startsWith("/addama/ui")) {
                callback = new StaticContentMemcacheLoaderCallback(registry);
            }

            byte[] content = (byte[]) memcacheServiceTemplate.loadIfNotExisting(requestUri, callback);
            if (content == null) {
                log.info("doFilter:not-static");
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.getOutputStream().write(content);
            log.info("doFilter:cached");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}


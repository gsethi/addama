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
package org.systemsbiology.addama.appengine.filters;

import com.google.appengine.api.users.UserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.systemsbiology.addama.appengine.util.ApiKeys.isAdmin;
import static org.systemsbiology.addama.appengine.util.ApiKeys.isValid;

/**
 * @author hrovira
 */
public class UserServiceFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(UserServiceFilter.class.getName());

    private final UserService userService = getUserService();

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (equalsIgnoreCase("get", request.getMethod()) && request.getRequestURI().startsWith("/addama/pubget")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (userService.isUserLoggedIn()) {
            //put user in the request, to be used later in the chain - and that way we are consistent with apikey case
            servletRequest.setAttribute("isAdmin", userService.isUserAdmin());
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String apiKey = request.getHeader("x-addama-apikey");
        if (!StringUtils.isEmpty(apiKey)) {
            if (isValid(apiKey, request.getRemoteAddr())) {
                servletRequest.setAttribute("isAdmin", isAdmin(apiKey));
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        // TODO : Check user agent and modify response accordingly
        log.info("not logged in; redirecting to login url");
//        response.setStatus(SC_UNAUTHORIZED);
        response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
    }
}

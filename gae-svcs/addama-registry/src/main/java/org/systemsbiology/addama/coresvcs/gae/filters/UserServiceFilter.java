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

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.systemsbiology.addama.coresvcs.gae.services.ApiKeys;
import org.systemsbiology.addama.coresvcs.gae.services.Users;

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
public class UserServiceFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(UserServiceFilter.class.getName());

    private ApiKeys apiKeys;
    private Users users;

    public void setApiKeys(ApiKeys apiKeys) {
        this.apiKeys = apiKeys;
    }

    public void setUsers(Users users){
        this.users = users;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        log.info("doFilter");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user != null) {
            //put user in the request, to be used later in the chain - and that way we are consistent with apikey case
            servletRequest.setAttribute("userUri",users.getLoggedInUserUri());
            servletRequest.setAttribute("isAdmin",users.isUserAdmin());
            log.fine("doFilter:" + user.getNickname() + "," + userService.isUserAdmin() + "," + userService.isUserLoggedIn());
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }


        String apiKey = request.getHeader("x-addama-apikey");
        log.fine("header:apikey=" + apiKey);
        if (StringUtils.isEmpty(apiKey)) {
            apiKey = request.getHeader("API_KEY"); // todo : deprecated
            log.fine("deprecated:apikey=" + apiKey);
        }

        if (!StringUtils.isEmpty(apiKey)) {
            if (apiKeys.isValidKey(apiKey, request.getRemoteAddr())) {
                servletRequest.setAttribute("userUri",apiKeys.getUserUriFromApiKey(apiKey));
                servletRequest.setAttribute("isAdmin",apiKeys.isKeyAdmin(apiKey));
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        // TODO : Check user agent and modify response accordingly
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
    }
}

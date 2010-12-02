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
public class AdminOnlyFilter extends GenericFilterBean {
    private static final Logger log = Logger.getLogger(AdminOnlyFilter.class.getName());

    private ApiKeys apiKeys;
    private Users users;

    public void setApiKeys(ApiKeys apiKeys) {
        this.apiKeys = apiKeys;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        log.fine("doFilter");

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (!isAdministrator(request)) {
            log.warning("doFilter: admin-only: " + request.getRequestURI());
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /*
     * Private Methods
     */

    private boolean isAdministrator(HttpServletRequest request) {
        if (users.isUserAdmin()) {
            return true;
        }

        String apiKey = request.getHeader("x-addama-apikey");
        if (StringUtils.isEmpty(apiKey)) {
            apiKey = request.getHeader("API_KEY"); // TODO : deprecated
        }

        if (!StringUtils.isEmpty(apiKey)) {
            return apiKeys.isKeyAdmin(apiKey);
        }

        return false;
    }
}

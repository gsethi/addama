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
package org.systemsbiology.addama.jcr.support;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springmodules.jcr.JcrTemplate;
import org.springmodules.jcr.SessionFactory;
import org.springmodules.jcr.support.OpenSessionInViewInterceptor;
import org.systemsbiology.addama.jcr.callbacks.JcrConnectionJsonConfigHandler;
import org.systemsbiology.addama.jsonconfig.JsonConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class JcrTemplateProvider extends OpenSessionInViewInterceptor {
    private static final Logger log = Logger.getLogger(JcrTemplateProvider.class.getName());
    protected final Map<String, SessionFactory> sessionFactorysByUri = new HashMap<String, SessionFactory>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new JcrConnectionJsonConfigHandler(sessionFactorysByUri));
    }

    /*
    pre controller
    */

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String workspaceUri = getWorkspaceUri(request);
            SessionFactory sf = sessionFactorysByUri.get(workspaceUri);
            if (sf == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
            super.setSessionFactory(sf);

            JcrTemplate jcrTemplate = new JcrTemplate(sf);
            jcrTemplate.afterPropertiesSet();
            request.setAttribute("JCR_TEMPLATE", jcrTemplate);

            return super.preHandle(request, response, handler);
        } catch (Exception e) {
            log.warning("unable to connect to repository: " + e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws DataAccessException {
        try {
            if (!StringUtils.equalsIgnoreCase("get", request.getMethod())) {
                JcrTemplate jcrTemplate = (JcrTemplate) request.getAttribute("JCR_TEMPLATE");
                if (jcrTemplate != null) {
                    boolean hasPendingChanges = jcrTemplate.hasPendingChanges();
                    if (hasPendingChanges) {
                        log.info("saving");
                        jcrTemplate.save();
                    }
                }
            }
            super.afterCompletion(request, response, handler, ex);
        } catch (Exception e) {
            log.warning(request.getRequestURI() + ":" + e);
        }
    }

    public static JcrTemplate getJcrTemplate(HttpServletRequest request) {
        return (JcrTemplate) request.getAttribute("JCR_TEMPLATE");
    }

    /*
    * Private Methods
    */

    private String getWorkspaceUri(HttpServletRequest request) {
        String requestUri = StringUtils.substringAfterLast(request.getRequestURI(), request.getContextPath());
        for (String uri : sessionFactorysByUri.keySet()) {
            if (requestUri.startsWith(uri)) {
                return uri;
            }
        }
        return null;
    }
}

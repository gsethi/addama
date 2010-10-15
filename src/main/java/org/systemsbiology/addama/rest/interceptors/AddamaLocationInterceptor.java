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
package org.systemsbiology.addama.rest.interceptors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springmodules.jcr.JcrSessionFactory;
import org.springmodules.jcr.JcrTemplate;
import org.springmodules.jcr.SessionFactory;
import org.springmodules.jcr.support.OpenSessionInViewInterceptor;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.registry.JsonConfigHandler;
import org.systemsbiology.addama.repositories.jcrrepo.JcrConnection;
import org.systemsbiology.addama.repositories.jcrrepo.register.JcrConnectionImpl;

import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class AddamaLocationInterceptor extends OpenSessionInViewInterceptor {
    private static final Logger log = Logger.getLogger(AddamaLocationInterceptor.class.getName());

    private static Map<String, SessionFactory> jcrCache = new HashMap<String, SessionFactory>();

    private JsonConfig jsonConfig;

    /*
    * Dependency Injection
    */

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    /*
    pre controller
    */

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String repositoryUri = getRepositoryUri(request);
        if (StringUtils.isEmpty(repositoryUri)) {
            return true;
        }

        try {
            SessionFactory sf = loadSessionFactory(repositoryUri);
            if (sf == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
            setSessionInJcrTemplate(handler, sf);

            return super.preHandle(request, response, handler);
        } catch (Exception e) {
            log.warning("unable to connect to repository: " + e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws DataAccessException {
        try {
            super.afterCompletion(request, response, handler, ex);
        } catch (IllegalStateException e) {
            log.warning("afterCompletion(" + request.getRequestURI() + "):" + e);
        }
    }

    /*
    * Private Methods
    */

    private String getRepositoryUri(HttpServletRequest request) {
        String repoName = StringUtils.substringBetween(request.getRequestURI() + "/", "/addama/repositories/", "/");
        if (!StringUtils.isEmpty(repoName)) {
            return "/addama/repositories/" + repoName;
        }
        return null;
    }

    private SessionFactory loadSessionFactory(String repositoryUri) throws Exception {
        SessionFactory sf = null;
        if (jcrCache.containsKey(repositoryUri) && jcrCache.get(repositoryUri) != null) {
            // get cached SessionFactory
            log.fine("getting cached template");
            sf = jcrCache.get(repositoryUri);
        } else {
            JcrConnection jcrConnection = getLocalConnection(repositoryUri);
            if (jcrConnection != null) {
                Repository repository = jcrConnection.getRepository();

                JcrSessionFactory jcrSF = new JcrSessionFactory();
                jcrSF.setRepository(repository);
                jcrSF.setCredentials(jcrConnection.getCredentials());
                jcrSF.afterPropertiesSet();

                jcrCache.put(repositoryUri, jcrSF);
                sf = jcrSF;
            }
        }

        if (sf != null) {
            super.setSessionFactory(sf);
        }
        return sf;
    }

    private JcrConnection getLocalConnection(String repositoryUri) throws Exception {
        JcrConnectionJsonConfigHandler configHandler = new JcrConnectionJsonConfigHandler(repositoryUri);
        jsonConfig.processConfiguration(configHandler);
        return configHandler.getJcrConnection();
    }

    private void setSessionInJcrTemplate(Object handler, SessionFactory sf) {
        try {
            // Reflection fun...need to set the loaded SessionFactory in the JcrTemplate that controllers have access to
            Class controllerClass = handler.getClass();
            Method setTemplateMethod = controllerClass.getMethod("setJcrTemplate", new Class[]{JcrTemplate.class});
            setTemplateMethod.invoke(handler, new JcrTemplate[]{new JcrTemplate(sf)});
        } catch (Exception e) {
            log.warning("setSessionInJcrTemplate(): " + handler.getClass() + ": " + e);
        }
    }

    /*
     * Private Classes
     */

    private class JcrConnectionJsonConfigHandler implements JsonConfigHandler {
        private final String repositoryUri;
        private JcrConnection jcrConnection;

        private JcrConnectionJsonConfigHandler(String repositoryUri) {
            this.repositoryUri = repositoryUri;
        }

        public void handle(JSONObject configuration) throws Exception {
            if (jcrConnection == null && configuration.has("locals")) {
                JSONArray locals = configuration.getJSONArray("locals");
                for (int i = 0; i < locals.length(); i++) {
                    JSONObject local = locals.getJSONObject(i);
                    if (StringUtils.equals(local.getString("uri"), repositoryUri)) {
                        String rmiserver = local.getString("rmiserver");
                        String username = local.getString("username");
                        String password = local.getString("password");
                        this.jcrConnection = new JcrConnectionImpl(rmiserver, username, password);
                        return;
                    }
                }
            }
        }

        public JcrConnection getJcrConnection() {
            return jcrConnection;
        }
    }
}

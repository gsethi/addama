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
package org.systemsbiology.addama.workspaces.callbacks;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springmodules.jcr.JcrSessionFactory;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import java.util.Map;

/**
 * @author hrovira
 */
public class JcrTemplateJsonConfigHandler implements JsonConfigHandler {
    private final Map<String, JcrTemplate> jcrTemplatesByUri;

    public JcrTemplateJsonConfigHandler(Map<String, JcrTemplate> map) {
        this.jcrTemplatesByUri = map;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("locals")) {
            JSONArray locals = configuration.getJSONArray("locals");
            for (int i = 0; i < locals.length(); i++) {
                JSONObject local = locals.getJSONObject(i);
                String uri = local.getString("uri");

                String rmiserver = local.getString("rmiserver");
                String username = local.getString("username");
                String password = local.getString("password");

                ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory();
                Repository repository = repositoryFactory.getRepository(rmiserver);

                JcrSessionFactory jcrSF = new JcrSessionFactory();
                jcrSF.setRepository(repository);
                jcrSF.setCredentials(new SimpleCredentials(username, password.toCharArray()));
                jcrSF.afterPropertiesSet();

                JcrTemplate jcrTemplate = new JcrTemplate(jcrSF);
                jcrTemplate.afterPropertiesSet();
                jcrTemplatesByUri.put(uri, jcrTemplate);
            }
        }

    }
}
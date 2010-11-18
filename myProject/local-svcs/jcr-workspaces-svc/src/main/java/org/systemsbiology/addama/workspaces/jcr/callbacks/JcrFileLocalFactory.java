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
package org.systemsbiology.addama.workspaces.jcr.callbacks;

import org.springframework.beans.factory.InitializingBean;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.workspaces.callbacks.FileLocalFactory;
import org.systemsbiology.addama.workspaces.callbacks.FileLocal;
import org.systemsbiology.addama.workspaces.callbacks.JcrTemplateJsonConfigHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hrovira
 */
public class JcrFileLocalFactory implements InitializingBean, FileLocalFactory {
    private final Map<String, JcrTemplate> jcrTemplatesByUri = new HashMap<String, JcrTemplate>();

    private JsonConfig jsonConfig;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new JcrTemplateJsonConfigHandler(jcrTemplatesByUri));
    }

    public FileLocal getFileLocal(String databaseUri, String localPath) {
        for (Map.Entry<String, JcrTemplate> entry : jcrTemplatesByUri.entrySet()) {
            if (databaseUri.startsWith(entry.getKey())) {
                return new JcrFileLocal(entry.getValue(), localPath, databaseUri);
            }
        }
        return null;
    }

}

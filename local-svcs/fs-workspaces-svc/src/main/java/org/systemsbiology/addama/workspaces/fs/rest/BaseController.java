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
package org.systemsbiology.addama.workspaces.fs.rest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.workspaces.fs.callbacks.RootPathsJsonConfigHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hrovira
 */
public abstract class BaseController extends AbstractController implements InitializingBean {
    protected final Map<String, String> rootPathsByUri = new HashMap<String, String>();

    protected JsonConfig jsonConfig;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new RootPathsJsonConfigHandler(rootPathsByUri));
    }

    /*
     * Protected Methods
     */

    protected String getUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return StringUtils.substringAfterLast(requestUri, request.getContextPath());
    }

    protected String getNodePath(String requestUri) {
        String nodePath = requestUri;
        nodePath = StringUtils.replace(nodePath, "%20", " ");
        nodePath = StringUtils.replace(nodePath, "+", " ");
        return nodePath;
    }

    protected String getRootPath(String uri) {
        for (Map.Entry<String, String> entry : rootPathsByUri.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }


}

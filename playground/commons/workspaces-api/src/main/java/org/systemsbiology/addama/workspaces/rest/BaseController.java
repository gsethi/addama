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
package org.systemsbiology.addama.workspaces.rest;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jcr.util.NodeUtil;
import org.systemsbiology.addama.registry.JsonConfig;
import org.systemsbiology.addama.workspaces.callbacks.RootPathsJsonConfigHandler;
import org.systemsbiology.addama.workspaces.callbacks.UriJsonConfigHandler;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author hrovira
 */
public abstract class BaseController extends AbstractController implements InitializingBean {
    protected final Map<String, String> rootPathsByUri = new HashMap<String, String>();
    protected final HashSet<String> workspaceUris = new HashSet<String>();

    protected JsonConfig jsonConfig;

    public void setJsonConfig(JsonConfig jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    public void afterPropertiesSet() throws Exception {
        jsonConfig.processConfiguration(new RootPathsJsonConfigHandler(rootPathsByUri));
        jsonConfig.processConfiguration(new UriJsonConfigHandler(workspaceUris));
    }

    /*
     * Protected Methods
     */

    protected String getUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return StringUtils.substringAfterLast(requestUri, request.getContextPath());
    }

    protected JcrTemplate getJcrTemplate(HttpServletRequest request) throws ResourceNotFoundException {
        return (JcrTemplate) request.getAttribute("JCR_TEMPLATE");
    }

    protected String getNodePath(String requestUri) {
        String nodePath = requestUri;
        nodePath = StringUtils.replace(nodePath, "%20", " ");
        nodePath = StringUtils.replace(nodePath, "+", " ");
        return nodePath;
    }

    protected void annotateNode(Node node, JSONObject json) throws Exception {
        if (json == null) return;

        Iterator itr = json.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            Object value = json.get(key);

            if (value != null) {
                if (value instanceof JSONObject) {
                    JSONObject childJson = (JSONObject) value;
                    Node childNode = NodeUtil.getNewNode(node, key);
                    annotateNode(childNode, childJson);
                } else {
                    setNodeProperty(node, key, value);
                }
            }
        }
    }

    protected void appendAnnotations(Node node, JSONObject json) throws Exception {
        PropertyIterator itr = node.getProperties();
        while (itr.hasNext()) {
            Property prop = itr.nextProperty();
            if (prop.getDefinition().isMultiple()) {
                loadProperty(json, prop, prop.getValues());
            } else {
                loadProperty(json, prop, prop.getValue());
            }
        }
    }

    protected void appendItems(Node node, JSONObject json) throws Exception {
        NodeIterator nodeItr = node.getNodes();
        while (nodeItr.hasNext()) {
            Node childNode = nodeItr.nextNode();
            JSONObject childJson = new JSONObject();
            childJson.put("name", childNode.getName());
            childJson.put("uri", childNode.getPath());
            if (isFileNode(childNode)) {
                childJson.put("isFile", true);
            }
            json.append("items", childJson);
        }
    }

    protected boolean isFileNode(Node node) throws Exception {
        return node.hasProperty("jcr:data");
    }

    protected String getRootPath(String uri) {
        for (Map.Entry<String, String> entry : rootPathsByUri.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /*
    * Private Methods
    */

    private void setNodeProperty(Node node, String key, Object value) throws Exception {
        if (node.hasProperty(key)) node.getProperty(key).remove();
        if (value == null || value.equals("null")) return;

        if (value instanceof Double[]) {
            Double[] jsonvalues = (Double[]) value;
            Value[] values = new Value[jsonvalues.length];
            for (int i = 0; i < jsonvalues.length; i++) {
                values[i] = new DoubleValue(jsonvalues[i]);
            }
            node.setProperty(key, values);

        } else if (value instanceof Double) {
            node.setProperty(key, (Double) value);

        } else if (value instanceof Long[]) {
            Long[] jsonvalues = (Long[]) value;
            Value[] values = new Value[jsonvalues.length];
            for (int i = 0; i < jsonvalues.length; i++) {
                values[i] = new LongValue(jsonvalues[i]);
            }
            node.setProperty(key, values);

        } else if (value instanceof Integer[]) {
            Integer[] jsonvalues = (Integer[]) value;
            Value[] values = new Value[jsonvalues.length];
            for (int i = 0; i < jsonvalues.length; i++) {
                values[i] = new LongValue(jsonvalues[i]);
            }
            node.setProperty(key, values);

        } else if (value instanceof Long) {
            node.setProperty(key, (Long) value);

        } else if (value instanceof Integer) {
            node.setProperty(key, (Integer) value);

        } else if (value instanceof Boolean[]) {
            Boolean[] jsonvalues = (Boolean[]) value;
            Value[] values = new Value[jsonvalues.length];
            for (int i = 0; i < jsonvalues.length; i++) {
                values[i] = new BooleanValue(jsonvalues[i]);
            }
            node.setProperty(key, values);

        } else if (value instanceof Boolean) {
            node.setProperty(key, (Boolean) value);

        } else if (value instanceof String[]) {
            node.setProperty(key, (String[]) value);

        } else {
            node.setProperty(key, value.toString());
        }
    }

    private void loadProperty(JSONObject json, Property property, Value... values) throws RepositoryException, JSONException {
        String propertyName = property.getName();

        if (propertyName.startsWith("jcr:")) return;

        for (Value value : values) {
            switch (property.getType()) {
                case PropertyType.STRING:
                    json.accumulate(propertyName, value.getString());
                    break;
                case PropertyType.DATE:
                    Date dt = value.getDate().getTime();
                    json.accumulate(propertyName, dt);
                    break;
                case PropertyType.DOUBLE:
                    json.accumulate(propertyName, value.getDouble());
                    break;
                case PropertyType.LONG:
                    json.accumulate(propertyName, value.getLong());
                    break;
                case PropertyType.BOOLEAN:
                    json.accumulate(propertyName, value.getBoolean());
                    break;
                default:
                    json.accumulate(propertyName, value.getString());
            }
        }
    }

}

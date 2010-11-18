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
package org.systemsbiology.addama.rest.json;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.systemsbiology.addama.jcr.util.NodeUtil;
import org.systemsbiology.addama.rest.util.UriBuilder;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


/**
 * @author hrovira
 */
public class BaseNodeJSONObject extends JSONObject {
    protected final Node node;
    protected final HttpServletRequest request;
    protected final UriBuilder uriBuilder;
    protected final DateFormat dateFormat;

    public BaseNodeJSONObject(Node node, HttpServletRequest request, DateFormat dateFormat) throws JSONException, RepositoryException {
        this.node = node;
        this.request = request;
        this.uriBuilder = new UriBuilder(request);
        this.dateFormat = dateFormat;

        String name = node.getName();
        if ("/".equals(name) || "".equals(name)) name = "root";

        put("name", name);
        put("path", node.getPath());

        uriBuilder.putUri(node, this);

        boolean isFile = NodeUtil.isFileNode(node);
        if (isFile) {
            put("size", NodeUtil.getFileSize(node));
            put("file", StringUtils.replace(getString("uri"), "/path/", "/file/"));
        }
        put("isFile", isFile);
        put("mimeType", NodeUtil.getMimeType(node));


    }

    /*
     * Protected Methods
     */

    protected void loadProperties() throws RepositoryException, JSONException {
        PropertyIterator itr = node.getProperties();
        while (itr.hasNext()) {
            Property prop = itr.nextProperty();
            if (prop.getDefinition().isMultiple()) {
                loadProperty(prop, prop.getValues());
            } else {
                loadProperty(prop, prop.getValue());
            }
        }
    }

    protected void loadSelectedProperties() throws RepositoryException, JSONException {
        HashSet<String> projections = new HashSet<String>();

        List<String> oldProjections = Arrays.asList(ServletRequestUtils.getStringParameters(request, "PROJECTION"));
        if (oldProjections != null) {
            projections.addAll(oldProjections);
        }

        String json = ServletRequestUtils.getStringParameter(request, "JSON", null);
        if (!StringUtils.isEmpty(json)) {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("projections")) {
                JSONArray projArray = jsonObject.getJSONArray("projections");
                for (int i = 0; i < projArray.length(); i++) {
                    projections.add(projArray.getString(i));
                }
            }
        }

        if (projections.isEmpty()) {
            return;
        }

        PropertyIterator itr = node.getProperties();
        while (itr.hasNext()) {
            Property prop = itr.nextProperty();
            String propName = prop.getName();
            if (projections.contains(propName)) {
                if (prop.getDefinition().isMultiple()) {
                    loadProperty(prop, prop.getValues());
                } else {
                    loadProperty(prop, prop.getValue());
                }
            }
        }
    }

    protected void appendChildren() throws RepositoryException, JSONException {
        if (NodeUtil.isFileNode(node)) return;

        NodeIterator itr = node.getNodes();
        while (itr.hasNext()) {
            Node nextNode = itr.nextNode();
            String name = nextNode.getName();
            if (nextNode.hasProperty("addama-uri")) {
                append("items", new AddamaUriNodeJSONObject(nextNode, request, dateFormat));
            } else if (nextNode.hasProperty("ref-repo") && nextNode.hasProperty("ref-path")) {
                throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
            } else if (nextNode.hasProperty("ref-uri")) {
                throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");
            } else if (!name.startsWith("jcr:")) {
                append("items", new CurrentNodeWithProjectionJSONObject(nextNode, request, dateFormat));
            }
        }
    }

    /*
    * Private Methods
    */

    private void loadProperty(Property property, Value... values) throws RepositoryException, JSONException {
        String propertyName = property.getName();

        if (propertyName.startsWith("jcr:")) return;
        if (StringUtils.equals("addama-uri", propertyName)) return;
        if (StringUtils.equals("size", propertyName)) return;
        if (StringUtils.equals("addama-type", propertyName)) return;

        for (Value value : values) {
            switch (property.getType()) {
                case PropertyType.STRING:
                    accumulate(propertyName, value.getString());
                    break;
                case PropertyType.BINARY:
                    put(propertyName, "BINARY_DATA");
                    break;
                case PropertyType.DATE:
                    Date dt = value.getDate().getTime();
                    if (dateFormat != null) {
                        accumulate(propertyName, dateFormat.format(dt));
                    } else {
                        accumulate(propertyName, dt);
                    }
                    break;
                case PropertyType.DOUBLE:
                    accumulate(propertyName, value.getDouble());
                    break;
                case PropertyType.LONG:
                    accumulate(propertyName, value.getLong());
                    break;
                case PropertyType.BOOLEAN:
                    accumulate(propertyName, value.getBoolean());
                    break;
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                    // TODO : Build uri?
                default:
                    accumulate(propertyName, value.getString());
            }
        }
    }

}
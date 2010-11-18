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
package org.systemsbiology.addama.services.repositories.mvc;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.ServletRequestUtils;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * @author hrovira
 */
public abstract class BaseController {
    protected DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    /*
     * Dependency Injection
     */

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /*
     * Protected Methods
     */

    protected Node getNode(HttpServletRequest request, String suffix) throws RepositoryException, ResourceNotFoundException {
        JcrTemplate jcrTemplate = getJcrTemplate(request);

        String requestUri = getDecodedRequestUri(request);
        if (StringUtils.contains(requestUri, "/path")) {
            String path = getPath(request, suffix);
            if (!jcrTemplate.itemExists(path)) {
                throw new ResourceNotFoundException(path);
            }
            return (Node) jcrTemplate.getItem(path);
        } else if (StringUtils.contains(requestUri, "/uuid")) {
            String uuid = StringUtils.substringAfter(requestUri, "/uuid/");
            return jcrTemplate.getNodeByUUID(uuid);
        } else {
            return jcrTemplate.getRootNode();
        }
    }

    protected JcrTemplate getJcrTemplate(HttpServletRequest request) throws ResourceNotFoundException {
        return (JcrTemplate) request.getAttribute("JCR_TEMPLATE");
    }

    protected void appendItems(String baseUri, Node node, JSONObject json, HttpServletRequest request) throws Exception {
        NodeIterator itr = node.getNodes();
        while (itr.hasNext()) {
            Node nextNode = itr.nextNode();
            String name = nextNode.getName();
            if (!name.startsWith("jcr:")) {
                JSONObject item = new JSONObject();
                if (StringUtils.contains(baseUri, "/path")) {
                    item.put("uri", baseUri + "/" + name);
                } else {
                    item.put("uri", baseUri + "/path/" + name);
                }
                item.put("name", nextNode.getName());
                item.put("path", nextNode.getPath());
                markAsFile(node, item);
                json.append("items", item);

                loadSelectedProperties(nextNode, item, request);
            }
        }
    }

    protected boolean markAsFile(Node node, JSONObject json) throws Exception {
        if (NodeUtil.isFileNode(node)) {
            json.put("size", NodeUtil.getFileSize(node));
            if (json.has("uri")) {
                json.put("file", StringUtils.replace(json.getString("uri"), "/path/", "/file/"));
            }
            json.put("mimeType", NodeUtil.getMimeType(node));
            json.put("isFile", true);
            return true;
        }
        json.put("isFile", false);
        return false;
    }

    protected void loadProperties(Node node, JSONObject json) throws Exception {
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

    protected void loadSelectedProperties(Node node, JSONObject json, HttpServletRequest request) throws Exception {
        HashSet<String> projections = new HashSet<String>();

        List<String> oldProjections = Arrays.asList(ServletRequestUtils.getStringParameters(request, "PROJECTION"));
        if (oldProjections != null) {
            projections.addAll(oldProjections);
        }

        String jsonparam = ServletRequestUtils.getStringParameter(request, "JSON", null);
        if (!StringUtils.isEmpty(jsonparam)) {
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
                    loadProperty(json, prop, prop.getValues());
                } else {
                    loadProperty(json, prop, prop.getValue());
                }
            }
        }
    }

    protected void loadProperty(JSONObject json, Property property, Value... values) throws RepositoryException, JSONException {
        String propertyName = property.getName();

        if (propertyName.startsWith("jcr:")) return;
        if (StringUtils.equals("addama-uri", propertyName)) return;
        if (StringUtils.equals("size", propertyName)) return;
        if (StringUtils.equals("addama-type", propertyName)) return;

        for (Value value : values) {
            switch (property.getType()) {
                case PropertyType.STRING:
                    json.accumulate(propertyName, value.getString());
                    break;
                case PropertyType.BINARY:
                    json.put(propertyName, "BINARY_DATA");
                    break;
                case PropertyType.DATE:
                    Date dt = value.getDate().getTime();
                    if (dateFormat != null) {
                        json.accumulate(propertyName, dateFormat.format(dt));
                    } else {
                        json.accumulate(propertyName, dt);
                    }
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

    /*
    * Private Methods
    */

    private String getPath(HttpServletRequest request, String suffix) throws RepositoryException, ResourceNotFoundException {
        String requestUri = getDecodedRequestUri(request);
        if (StringUtils.contains(requestUri, "/path")) {
            if (StringUtils.isEmpty(suffix)) suffix = null;

            String path = StringUtils.substringAfter(requestUri, "/path");
            if (StringUtils.contains(requestUri, suffix)) {
                path = StringUtils.substringBetween(requestUri, "/path", suffix);
            }

            if (StringUtils.isEmpty(path)) path = "/";
            return path;
        }

        if (StringUtils.contains(requestUri, "/uuid")) {
            JcrTemplate jcrTemplate = getJcrTemplate(request);

            String uuid = StringUtils.substringAfter(requestUri, "/uuid/");
            Node node = jcrTemplate.getNodeByUUID(uuid);
            return node.getPath();
        }

        return "/";
    }

    private String getDecodedRequestUri(HttpServletRequest request) {
        try {
            return URLDecoder.decode(request.getRequestURI(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

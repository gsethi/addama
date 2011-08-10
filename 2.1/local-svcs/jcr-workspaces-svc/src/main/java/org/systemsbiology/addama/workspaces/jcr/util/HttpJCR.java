package org.systemsbiology.addama.workspaces.jcr.util;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Iterator;

/**
 * @author hrovira
 */
public class HttpJCR {
    public static String getUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return StringUtils.substringAfterLast(requestUri, request.getContextPath());
    }

    public static String getNodePath(String requestUri) {
        String nodePath = requestUri;
        nodePath = StringUtils.replace(nodePath, "%20", " ");
        nodePath = StringUtils.replace(nodePath, "+", " ");
        return nodePath;
    }

    public static void annotateNode(Node node, JSONObject json) throws Exception {
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

    public static void appendAnnotations(Node node, JSONObject json) throws Exception {
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

    public static void appendItems(Node node, JSONObject json) throws Exception {
        NodeIterator nodeItr = node.getNodes();
        while (nodeItr.hasNext()) {
            Node childNode = nodeItr.nextNode();
            JSONObject childJson = new JSONObject();
            childJson.put("name", childNode.getName());
            childJson.put("uri", childNode.getPath());
            if (childNode.hasProperty("jcr:data")) {
                childJson.put("isFile", true);
            }
            json.append("items", childJson);
        }
    }

    /*
    * Private Methods
    */

    private static void setNodeProperty(Node node, String key, Object value) throws Exception {
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

    private static void loadProperty(JSONObject json, Property property, Value... values) throws RepositoryException, JSONException {
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

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
package org.systemsbiology.addama.rest.transforms;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import static org.systemsbiology.addama.rest.AddamaKeywords.*;

/**
 * @author hrovira
 */
public class CreateOrUpdateNode {
    private final Calendar currentDate = Calendar.getInstance();

    /*
     * Public Methods
     */

    public void doCreate(Node node, JSONObject jsonObject) throws JSONException, RepositoryException, IOException, InvalidSyntaxException, ParseException {
        if (jsonObject == null) return;

        tagCreatedOrModified(node);

        Iterator itr = jsonObject.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            Object value = jsonObject.get(key);

            if (value != null) {
                if (isReservedKeyword(key)) {
                    // ignore: do not persist
                } else if (StringUtils.equalsIgnoreCase("addama-ref-uri", key)) {
                    throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");

                } else if (value instanceof JSONObject) {
                    JSONObject childJson = (JSONObject) value;
                    if (childJson.has(addama_date.word())) {
                        setAddamaDate(node, key, childJson);
                    } else if (childJson.has("addama-ref")) {
                        throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
                    } else if (childJson.has("addama-ref-uri")) {
                        throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");
                    } else {
                        Node childNode = NodeUtil.getNewNode(node, key);
                        doCreate(childNode, childJson);
                    }

                } else if (value instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) value;
                    doCreate(node, key, jsonArray);
                } else if (addama_uri.isContainedIn(value.toString())) {
                    setAddamaUri(node, key, value);
                    // TODO : Handle addama-uri arrays

                } else if (addama_generate_date.isEqual(value.toString())) {
                    node.setProperty(key, Calendar.getInstance());

                } else {
                    setNodeProperty(node, key, value);
                }
            }
        }
    }

    public void doUpdate(Node node, JSONObject jsonObject) throws JSONException, RepositoryException, IOException, InvalidSyntaxException, ParseException {
        if (jsonObject == null) return;

        tagCreatedOrModified(node);

        Iterator itr = jsonObject.keys();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            Object value = jsonObject.get(key);
            if (value != null && !"null".equals(value)) {
                if (isReservedKeyword(key)) {
                    // ignore: do not persist

                } else if (StringUtils.equalsIgnoreCase("addama-ref-uri", key)) {
                    throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");

                } else if (value instanceof JSONObject) {
                    JSONObject childJson = (JSONObject) value;
                    if (childJson.has(addama_date.word())) {
                        setAddamaDate(node, key, childJson);
                    } else if (childJson.has("addama-ref")) {
                        throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
                    } else if (childJson.has("addama-ref-uri")) {
                        throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");
                    } else {
                        Node childNode = NodeUtil.getNewNode(node, key);
                        doCreate(childNode, (JSONObject) value);
                    }

                } else if (value instanceof JSONArray) {
                    if (node.hasNode(key)) throw new InvalidSyntaxException(key + " cannot be modified");
                    doCreate(node, key, (JSONArray) value);

                } else if (addama_uri.isContainedIn(value.toString())) {
                    setAddamaUri(node, key, value);
                    // TODO : Handle addama-uri arrays

                } else if (addama_generate_date.isEqual(value.toString())) {
                    node.setProperty(key, Calendar.getInstance());

                } else {
                    setNodeProperty(node, key, value);
                }
            } else {
                if (node.hasNode(key)) throw new InvalidSyntaxException(key + " cannot be removed");
                setNodeProperty(node, key, value);
            }
        }
    }

    /*
     * Private Methods
     */

    private void doCreate(Node node, String key, JSONArray jsonArray) throws JSONException, RepositoryException, IOException, InvalidSyntaxException, ParseException {
        if (jsonArray == null) return;

        // scan array, determine if jsonobject or property
        JSONArrayTransform transform = new JSONArrayTransform(jsonArray);
        JSONArrayTransform.ObjectType objectType = transform.getObjectType();
        switch (objectType) {
            case JSON:
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject childJson = jsonArray.getJSONObject(i);
                    if (childJson.has("addama-ref")) {
                        throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
                    } else if (childJson.has("addama-ref-uri")) {
                        throw new IllegalArgumentException("addama-ref-uri is deprecated, use addama-uri instead");
                    } else {
                        Node childNode = NodeUtil.getNewNode(node, key);
                        doCreate(childNode, childJson);
                    }
                }
                break;
            case Double:
                setNodeProperty(node, key, transform.getDoubles());
                break;
            case Long:
                setNodeProperty(node, key, transform.getLongs());
                break;
            case Integer:
                setNodeProperty(node, key, transform.getIntegers());
                break;
            case Boolean:
                setNodeProperty(node, key, transform.getBooleans());
                break;
            case String:
                setNodeProperty(node, key, transform.getStrings());
                break;
        }
    }

    private void setNodeProperty(Node node, String key, Object value) throws RepositoryException {
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

    private void setAddamaDate(Node node, String propertyName, JSONObject json) throws JSONException, ParseException, RepositoryException {
        JSONObject dateJson = json.getJSONObject(addama_date.word());

        SimpleDateFormat sdf = new SimpleDateFormat(dateJson.getString(addama_date_pattern.word()));
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdf.parse(dateJson.getString(addama_date_value.word())));
        node.setProperty(propertyName, cal);
    }

    private void setAddamaUri(Node node, String key, Object value) throws JSONException, RepositoryException {
        String addamaUri = ((String) value).replaceAll(addama_uri.word() + ":", "");

        Node keyNode = NodeUtil.getNodeNewIfNotExists(node, key);
        keyNode.setProperty(addama_uri.word(), addamaUri);
    }

    private boolean isReservedKeyword(String key) {
        if (StringUtils.equalsIgnoreCase("REDIRECT_TO", key)) return true;
        if (StringUtils.equalsIgnoreCase(created_at.word(), key)) return true;
        if (StringUtils.equalsIgnoreCase(created_by.word(), key)) return true;
        if (StringUtils.equalsIgnoreCase(last_modified_at.word(), key)) return true;
        if (StringUtils.equalsIgnoreCase(last_modified_by.word(), key)) return true;
        return false;
    }

    private void tagCreatedOrModified(Node node) throws RepositoryException {
        if (node.hasProperty(created_at.word())) {
            node.setProperty(last_modified_at.word(), currentDate);
        } else {
            node.setProperty(created_at.word(), currentDate);
        }
    }
}

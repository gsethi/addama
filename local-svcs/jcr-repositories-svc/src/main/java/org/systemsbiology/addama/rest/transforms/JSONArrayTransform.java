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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;

import java.util.ArrayList;

/**
 * @author hrovira
 */
class JSONArrayTransform {
    private final ArrayList<Object> items = new ArrayList<Object>();
    private ObjectType objectType;

    /*
     * Constructors
     */
    public JSONArrayTransform(JSONArray jsonArray) throws JSONException, InvalidSyntaxException {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object item = jsonArray.get(i);
                if (item != null) {
                    if (item instanceof JSONObject) {
                        setObjectType(ObjectType.JSON);
                    } else if (item instanceof Double) {
                        setObjectType(ObjectType.Double);
                    } else if (item instanceof Long) {
                        setObjectType(ObjectType.Long);
                    } else if (item instanceof Integer) {
                        setObjectType(ObjectType.Integer);
                    } else if (item instanceof Boolean) {
                        setObjectType(ObjectType.Boolean);
                    } else {
                        setObjectType(ObjectType.String);
                    }
                    items.add(item);
                }
            }
        }
    }

    /*
     * Public Methods
     */
    public ObjectType getObjectType() {
        if (objectType == null) return ObjectType.String;
        return objectType;
    }

    public Double[] getDoubles() {
        return items.toArray(new Double[items.size()]);
    }

    public Long[] getLongs() {
        return items.toArray(new Long[items.size()]);
    }

    public Integer[] getIntegers() {
        return items.toArray(new Integer[items.size()]);
    }

    public Boolean[] getBooleans() {
        return items.toArray(new Boolean[items.size()]);
    }

    public String[] getStrings() {
        return items.toArray(new String[items.size()]);
    }

    /*
     * Private Methods
     */
    private void setObjectType(ObjectType newObjectType) throws InvalidSyntaxException {
        if (objectType != null && objectType != newObjectType) {
            throw new InvalidSyntaxException("array has mixed types");
        }
        objectType = newObjectType;
    }

    /*
     * Inner Enumeration
     */
    public enum ObjectType {
        JSON, Double, Long, Integer, Boolean, String
    }
}

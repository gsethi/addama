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
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.rest.AddamaKeywords;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author hrovira
 */
public class NodeAnnotationsJSONObject extends JSONObject {
    protected final Node node;
    protected final HttpServletRequest request;
    protected final DateFormat dateFormat;

    public NodeAnnotationsJSONObject(Node node, HttpServletRequest request, DateFormat dateFormat) throws JSONException, RepositoryException {
        this.node = node;
        this.request = request;
        this.dateFormat = dateFormat;
        this.loadProperties();
    }

    /*
     * Protected Methods
     */
    protected void loadProperties() throws RepositoryException, JSONException {
        PropertyIterator itr = node.getProperties();
        while (itr.hasNext()) {
            boolean isCustom = true;
            Property prop = itr.nextProperty();
            //filter out all AddamaKeywords
            for (AddamaKeywords kw : AddamaKeywords.values()) {
                if (prop.getName().equalsIgnoreCase(kw.word())) {
                    isCustom = false;
                    break;
                }
            }
            if (isCustom) {
                if (prop.getDefinition().isMultiple()) {
                    loadProperty(prop, prop.getValues());
                } else {
                    loadProperty(prop, prop.getValue());
                }
            }
        }
    }


    /*
    * Private Methods
    */
    private void loadProperty(Property property, Value... values) throws RepositoryException, JSONException {
        String propertyName = property.getName();

        if (propertyName.startsWith("jcr:")) return;
        if (StringUtils.equals(propertyName, "addama-uri")) return;

        for (Value value : values) {
            switch (property.getType()) {
                case PropertyType.STRING:
                    accumulate(propertyName, value.getString());
                    break;
                case PropertyType.BINARY:
                    accumulate(propertyName, "BINARY_DATA");
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
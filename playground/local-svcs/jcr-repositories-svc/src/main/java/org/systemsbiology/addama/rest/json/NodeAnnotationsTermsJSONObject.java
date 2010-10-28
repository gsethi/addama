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

import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.rest.util.UriBuilder;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;

/**
 * @author hrovira
 */
public class NodeAnnotationsTermsJSONObject extends JSONObject {
    protected final Node node;
    protected final HttpServletRequest request;
    protected final UriBuilder uriBuilder;
    protected final DateFormat dateFormat;

    public NodeAnnotationsTermsJSONObject(Node node, HttpServletRequest request, DateFormat dateFormat) throws JSONException, RepositoryException {
        this.node = node;
        this.request = request;
        this.uriBuilder = new UriBuilder(request);
        this.dateFormat = dateFormat;

        PropertyIterator itr = node.getProperties();
        while (itr.hasNext()) {
            Property prop = itr.nextProperty();
            String propName = prop.getName();
            if (!propName.startsWith("jcr:")) {
                this.append("terms", prop.getName());
            }

        }
    }

}
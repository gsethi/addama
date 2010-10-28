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
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;

import static org.systemsbiology.addama.rest.AddamaKeywords.addama_uri;

/**
 * @author hrovira
 */
public class NodeStructuredJSONObject extends BaseNodeJSONObject {
    public NodeStructuredJSONObject(Node node, HttpServletRequest request, DateFormat dateFormat, JcrTemplate jcrTemplate) throws JSONException, RepositoryException {
        super(node, request, dateFormat);
        loadProperties();

        NodeIterator itr = node.getNodes();
        while (itr.hasNext()) {
            Node nextNode = itr.nextNode();
            String name = nextNode.getName();
            if (NodeUtil.isFileNode(nextNode)) {
                accumulate(name, new CurrentNodeWithProjectionJSONObject(nextNode, request, dateFormat));
            } else if (!name.startsWith("jcr:")) {
                if (nextNode.hasProperty(addama_uri.word())) {
                    // TODO : Extend based on URI
                    accumulate(name, new AddamaUriNodeJSONObject(nextNode, request, dateFormat));
                } else {
                    accumulate(name, new NodeStructuredJSONObject(nextNode, request, dateFormat, jcrTemplate));
                }

                if (nextNode.hasProperty("ref-repo") && nextNode.hasProperty("ref-path")) {
                    throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
                }
                if (nextNode.hasProperty("ref-uri")) {
                    throw new IllegalArgumentException("addama-ref is deprecated, use addama-uri instead");
                }
            }
        }
    }
}
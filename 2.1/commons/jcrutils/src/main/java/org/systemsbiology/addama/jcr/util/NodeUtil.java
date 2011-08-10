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
package org.systemsbiology.addama.jcr.util;

import org.apache.commons.lang.StringUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * @author hrovira
 */
public class NodeUtil {

    public static Node getNewNode(Node parentNode, String name) throws RepositoryException {
        String validName = name.replaceAll("'", "");

        Node newNode = parentNode.addNode(validName);
        newNode.addMixin("mix:referenceable");
        return newNode;
    }

    public static Node getNodeNewIfNotExists(Node parentNode, String name) throws RepositoryException {
        String validName = name.replaceAll("'", "");

        if (parentNode.hasNode(validName)) {
            return parentNode.getNode(validName);
        }

        return getNewNode(parentNode, validName);
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        if (node.hasProperty("addama-type")) {
            return node.getProperty("addama-type").getString().equals("addama-file");
        }

        NodeType nodeType = node.getPrimaryNodeType();
        return StringUtils.equals("nt:file", nodeType.getName());
    }

    public static String getMimeType(Node node) throws RepositoryException {
        if (isFileNode(node)) {
            if (node.hasNode("jcr:content")) {
                Node contentNode = node.getNode("jcr:content");
                if (contentNode.hasProperty("jcr:mimeType")) {
                    Property property = contentNode.getProperty("jcr:mimeType");
                    if (property != null) return property.getString();
                }
            } else {
                if (node.hasProperty("jcr:mimeType")) {
                    Property property = node.getProperty("jcr:mimeType");
                    if (property != null) return property.getString();
                }
            }
        }
        return null;
    }

    public static long getFileSize(Node node) throws RepositoryException {
        if (isFileNode(node)) {
            if (node.hasProperty("file_size")) {
                Property property = node.getProperty("file_size");
                if (property != null) {
                    return property.getLong();
                }
            }
        }

        return 0;
    }

    public static Node getParent(Node node) throws RepositoryException {
        if (node != null && !StringUtils.equals(node.getPath(), "/")) {
            return node.getParent();
        }
        return null;
    }

}

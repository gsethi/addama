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
package org.systemsbiology.addama.jcr.callbacks;

import org.apache.commons.lang.StringUtils;
import org.springmodules.jcr.JcrCallback;
import org.systemsbiology.addama.jcr.util.NodeUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;

/**
 * @author hrovira
 */
public class GetNodeAtPathJcrCallback implements JcrCallback {
    private final String nodePath;

    /*
     * Constructors
     */
    public GetNodeAtPathJcrCallback(String nodePath) {
        this.nodePath = nodePath;
    }

    /*
    * Public Methods
    */
    public Object doInJcr(Session session) throws IOException, RepositoryException {
        if (session.itemExists(nodePath)) {
            return session.getItem(nodePath);
        }

        Node rootNode = session.getRootNode();
        String rootPath = rootNode.getPath();

        Node nodeAtPath = rootNode;
        for (String split : nodePath.split("/")) {
            if (!StringUtils.isEmpty(split) && !StringUtils.equals(rootPath, split)) {
                nodeAtPath = NodeUtil.getNodeNewIfNotExists(nodeAtPath, split);
            }
        }

        return nodeAtPath;
    }
}
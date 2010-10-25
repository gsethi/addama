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
package org.systemsbiology.addama.rest.util;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hrovira
 */
public class ResultsCollector {
    private final HashMap<String, Node> nodesByUuid = new HashMap<String, Node>();

    public void addQueryResult(QueryResult queryResult) throws RepositoryException {
        NodeIterator itr = queryResult.getNodes();
        while (itr.hasNext()) {
            Node node = itr.nextNode();
            nodesByUuid.put(node.getUUID(), node);
        }
    }

    public boolean hasResults() {
        return !nodesByUuid.isEmpty();
    }

    public int getNumberOfResults() {
        return nodesByUuid.size();
    }

    public Set<String> getUuids() {
        return nodesByUuid.keySet();
    }

    public Map<String, Node> getNodesByUuid() {
        return nodesByUuid;
    }
}

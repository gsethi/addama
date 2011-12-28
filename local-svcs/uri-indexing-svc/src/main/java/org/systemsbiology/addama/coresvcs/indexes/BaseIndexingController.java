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
package org.systemsbiology.addama.coresvcs.indexes;

import org.springmodules.lucene.index.core.LuceneIndexTemplate;
import org.springmodules.lucene.search.core.LuceneSearchTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.coresvcs.indexes.handlers.LuceneIndexTemplateMappingsHandler;
import org.systemsbiology.addama.coresvcs.indexes.handlers.LuceneSearchTemplateMappingsHandler;
import org.systemsbiology.addama.jsonconfig.ServiceConfig;

import java.util.HashMap;

/**
 * @author hrovira
 */
public abstract class BaseIndexingController {
    private final HashMap<String, LuceneSearchTemplate> searchTemplates = new HashMap<String, LuceneSearchTemplate>();
    private final HashMap<String, LuceneIndexTemplate> indexTemplates = new HashMap<String, LuceneIndexTemplate>();

    public void setServiceConfig(ServiceConfig serviceConfig) throws Exception {
        serviceConfig.visit(new LuceneSearchTemplateMappingsHandler(searchTemplates));
        serviceConfig.visit(new LuceneIndexTemplateMappingsHandler(indexTemplates));
    }

    /*
     * Protected Methods
     */

    protected LuceneSearchTemplate getLuceneSearchTemplate(String indexId) throws ResourceNotFoundException {
        LuceneSearchTemplate lst = searchTemplates.get(indexId);
        if (lst != null) {
            return lst;
        }
        throw new ResourceNotFoundException(indexId);
    }

    protected LuceneIndexTemplate getLuceneIndexTemplate(String indexId) throws ResourceNotFoundException {
        LuceneIndexTemplate lit = indexTemplates.get(indexId);
        if (lit != null) {
            return lit;
        }
        throw new ResourceNotFoundException(indexId);
    }
}

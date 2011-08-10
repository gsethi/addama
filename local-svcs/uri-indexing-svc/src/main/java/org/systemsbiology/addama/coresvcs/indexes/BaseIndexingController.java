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

import org.apache.commons.lang.StringUtils;
import org.springmodules.lucene.index.core.LuceneIndexTemplate;
import org.springmodules.lucene.search.core.LuceneSearchTemplate;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.coresvcs.indexes.handlers.LuceneIndexTemplateJsonConfigHandler;
import org.systemsbiology.addama.coresvcs.indexes.handlers.LuceneSearchTemplateJsonConfigHandler;
import org.systemsbiology.addama.jsonconfig.JsonConfig;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Set;

/**
 * @author hrovira
 */
public abstract class BaseIndexingController {
    private final HashMap<String, LuceneSearchTemplate> searchTemplatesByUri = new HashMap<String, LuceneSearchTemplate>();
    private final HashMap<String, LuceneIndexTemplate> indexTemplatesByUri = new HashMap<String, LuceneIndexTemplate>();

    public void setJsonConfig(JsonConfig jsonConfig) {
        jsonConfig.visit(new LuceneSearchTemplateJsonConfigHandler(searchTemplatesByUri));
        jsonConfig.visit(new LuceneIndexTemplateJsonConfigHandler(indexTemplatesByUri));
    }

    /*
     * Protected Methods
     */

    protected LuceneSearchTemplate getLuceneSearchTemplate(HttpServletRequest request) throws ResourceNotFoundException {
        String mappingUri = getMappingUri(request, searchTemplatesByUri.keySet());
        if (!StringUtils.isEmpty(mappingUri)) {
            return searchTemplatesByUri.get(mappingUri);
        }
        throw new ResourceNotFoundException(getUri(request));
    }

    protected LuceneIndexTemplate getLuceneIndexTemplate(HttpServletRequest request) throws ResourceNotFoundException {
        String mappingUri = getMappingUri(request, indexTemplatesByUri.keySet());
        if (!StringUtils.isEmpty(mappingUri)) {
            return indexTemplatesByUri.get(mappingUri);
        }
        throw new ResourceNotFoundException(getUri(request));
    }

    /*
     * Private Methods
     */

    private String getMappingUri(HttpServletRequest request, Set<String> mappings) {
        String uri = getUri(request);
        for (String key : mappings) {
            if (uri.startsWith(key)) {
                return key;
            }
        }
        return null;
    }

    private String getUri(HttpServletRequest request) {
        return StringUtils.substringAfter(request.getRequestURI(), request.getContextPath());
    }
}

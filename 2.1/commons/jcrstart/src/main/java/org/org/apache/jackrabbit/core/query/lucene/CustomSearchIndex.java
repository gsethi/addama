package org.org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.lucene.document.Document;

import javax.jcr.RepositoryException;

/**
 * @author hrovira
 */
public class CustomSearchIndex extends SearchIndex {
    protected Document createDocument(NodeState node,
                                      NamespaceMappings nsMappings,
                                      IndexFormatVersion indexFormatVersion)
            throws RepositoryException {
        CustomNodeIndexer indexer = new CustomNodeIndexer(node,
                getContext().getItemStateManager(), nsMappings, getTextExtractor());
        indexer.setSupportHighlighting(getSupportHighlighting());
        indexer.setIndexingConfiguration(getIndexingConfig());
        indexer.setIndexFormatVersion(indexFormatVersion);
        Document doc = indexer.createDoc();
        mergeAggregatedNodeIndexes(node, doc);
        return doc;
    }
}

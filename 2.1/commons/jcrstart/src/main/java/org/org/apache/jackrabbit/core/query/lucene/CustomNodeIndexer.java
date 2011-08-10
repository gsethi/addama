package org.org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.NodeIndexer;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * @author hrovira
 */
public class CustomNodeIndexer extends NodeIndexer {
    public CustomNodeIndexer(NodeState node, ItemStateManager stateProvider, NamespaceMappings mappings, TextExtractor extractor) {
        super(node, stateProvider, mappings, extractor);
    }

    protected Document createDoc() throws RepositoryException {
        Document doc = new Document();

        doc.setBoost(getNodeBoost());

        // special fields
        // UUID
        doc.add(new Field(FieldNames.UUID, node.getNodeId().getUUID().toString(), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        try {
            // parent UUID
            if (node.getParentId() == null) {
                // root node
                doc.add(new Field(FieldNames.PARENT, "", Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                doc.add(new Field(FieldNames.LABEL, "", Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
            } else {
                doc.add(new Field(FieldNames.PARENT, node.getParentId().toString(), Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
                NodeState parent = (NodeState) stateProvider.getItemState(node.getParentId());
                NodeState.ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
                if (child == null) {
                    // this can only happen when jackrabbit
                    // is running in a cluster.
                    throw new RepositoryException("Missing child node entry " +
                            "for node with id: " + node.getNodeId());
                }
                String name = resolver.getJCRName(child.getName());
                doc.add(new Field(FieldNames.LABEL, name, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
                doc.add(new Field("name", name, Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
            }
        } catch (NoSuchItemStateException e) {
            throwRepositoryException(e);
        } catch (ItemStateException e) {
            throwRepositoryException(e);
        } catch (NamespaceException e) {
            // will never happen, because this.mappings will dynamically add
            // unknown uri<->prefix mappings
        }

        for (Object prop : node.getPropertyNames()) {
            Name propName = (Name) prop;
            PropertyId id = new PropertyId(node.getNodeId(), propName);
            try {
                PropertyState propState = (PropertyState) stateProvider.getItemState(id);

                // add each property to the _PROPERTIES_SET for searching
                // beginning with V2
                if (indexFormatVersion.getVersion()
                        >= IndexFormatVersion.V2.getVersion()) {
                    addPropertyName(doc, propState.getName());
                }

                InternalValue[] values = propState.getValues();
                for (InternalValue value : values) {
                    addValue(doc, value, propState.getName());
                }
                if (values.length > 1) {
                    // real multi-valued
                    addMVPName(doc, propState.getName());
                }
            } catch (NoSuchItemStateException e) {
                throwRepositoryException(e);
            } catch (ItemStateException e) {
                throwRepositoryException(e);
            }
        }
        return doc;
    }

    private void throwRepositoryException(Exception e)
            throws RepositoryException {
        String msg = "Error while indexing node: " + node.getNodeId() + " of "
                + "type: " + node.getNodeTypeName();
        throw new RepositoryException(msg, e);
    }

    /**
     * Adds a {@link FieldNames#MVP} field to <code>doc</code> with the resolved
     * <code>name</code> using the internal search index namespace mapping.
     *
     * @param doc  the lucene document.
     * @param name the name of the multi-value property.
     */
    private void addMVPName(Document doc, Name name) {
        try {
            String propName = resolver.getJCRName(name);
            doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
        } catch (NamespaceException e) {
            // will never happen, prefixes are created dynamically
        }
    }

    /**
     * Adds a value to the lucene Document.
     *
     * @param doc   the document.
     * @param value the internal jackrabbit value.
     * @param name  the name of the property.
     */
    private void addValue(Document doc, InternalValue value, Name name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        switch (value.getType()) {
            case PropertyType.BINARY:
                if (isIndexed(name)) {
                    addBinaryValue(doc, fieldName, value.getBLOBFileValue());
                }
                break;
            case PropertyType.BOOLEAN:
                if (isIndexed(name)) {
                    addBooleanValue(doc, fieldName, Boolean.valueOf(value.getBoolean()));
                }
                break;
            case PropertyType.DATE:
                if (isIndexed(name)) {
                    addCalendarValue(doc, fieldName, value.getDate());
                }
                break;
            case PropertyType.DOUBLE:
                if (isIndexed(name)) {
                    addDoubleValue(doc, fieldName, new Double(value.getDouble()));
                }
                break;
            case PropertyType.LONG:
                if (isIndexed(name)) {
                    addLongValue(doc, fieldName, new Long(value.getLong()));
                }
                break;
            case PropertyType.REFERENCE:
                if (isIndexed(name)) {
                    addReferenceValue(doc, fieldName, value.getUUID());
                }
                break;
            case PropertyType.PATH:
                if (isIndexed(name)) {
                    addPathValue(doc, fieldName, value.getPath());
                }
                break;
            case PropertyType.STRING:
                if (isIndexed(name)) {
                    // never fulltext index jcr:uuid String
                    if (name.equals(NameConstants.JCR_UUID)) {
                        addStringValue(doc, fieldName, value.getString(),
                                false, false, DEFAULT_BOOST);
                    } else {
                        addStringValue(doc, fieldName, value.getString(),
                                true, isIncludedInNodeIndex(name),
                                getPropertyBoost(name));
                    }
                }
                break;
            case PropertyType.NAME:
                // jcr:primaryType and jcr:mixinTypes are required for correct
                // node type resolution in queries
                if (isIndexed(name) ||
                        name.equals(NameConstants.JCR_PRIMARYTYPE) ||
                        name.equals(NameConstants.JCR_MIXINTYPES)) {
                    addNameValue(doc, fieldName, value.getQName());
                }
                break;
            default:
                throw new IllegalArgumentException("illegal internal value type");
        }
    }

    private void addPropertyName(Document doc, Name name) {
        String fieldName = name.getLocalName();
        try {
            fieldName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        doc.add(new Field(FieldNames.PROPERTIES_SET, fieldName, Field.Store.NO, Field.Index.NO_NORMS));
    }

}

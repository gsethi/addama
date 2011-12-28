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
package org.systemsbiology.addama.coresvcs.indexes.batches;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springmodules.lucene.index.core.LuceneIndexTemplate;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class ReferenceJsonBatchCallback implements BatchCallback {
    private static final Logger log = Logger.getLogger(ReferenceJsonBatchCallback.class.getName());

    private final LuceneIndexTemplate indexTemplate;

    public ReferenceJsonBatchCallback(LuceneIndexTemplate indexTemplate) {
        this.indexTemplate = indexTemplate;
    }

    /*
     * BatchCallback
     */

    public void createIndex(BatchItem batchItem, JSONObject refJson) throws Exception {
        if (log.isLoggable(Level.FINE)) {
            log.fine("createIndex(" + batchItem.getBatchUri() + "," + refJson + ")");
        }

        if (!refJson.has("uri")) {
            log.warning("createIndex(" + batchItem.getBatchUri() + "," + refJson + "): does not specify uri");
            return;
        }

        String uri = refJson.getString("uri");

        Document document = new Document();
        document.add(new Field("uri", uri, Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("batch", batchItem.getBatchUri(), Field.Store.YES, Field.Index.UN_TOKENIZED));
        if (refJson.has("name")) {
            document.add(new Field("name", refJson.getString("name"), Field.Store.YES, Field.Index.UN_TOKENIZED));
        } else {
            if (contains(uri, "/")) {
                if (uri.endsWith("/")) {
                    String name = substringAfterLast(substringBeforeLast(uri, "/"), "/");
                    document.add(new Field("name", name, Field.Store.YES, Field.Index.UN_TOKENIZED));
                } else {
                    String name = substringAfterLast(uri, "/");
                    document.add(new Field("name", name, Field.Store.YES, Field.Index.UN_TOKENIZED));
                }
            }
        }
        if (refJson.has("label")) {
            document.add(new Field("label", refJson.getString("label"), Field.Store.YES, Field.Index.UN_TOKENIZED));
        }

        if (refJson.has("annotations")) {
            JSONObject annotations = refJson.getJSONObject("annotations");
            Iterator itr = annotations.keys();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                String value = annotations.getString(key);
                document.add(new Field(key, value, Field.Store.YES, Field.Index.TOKENIZED));
            }
        }

        if (refJson.has("keywords")) {
            JSONArray keywords = refJson.getJSONArray("keywords");
            for (int i = 0; i < keywords.length(); i++) {
                document.add(new Field("keywords", keywords.getString(i), Field.Store.YES, Field.Index.TOKENIZED));
            }
        }

        if (refJson.has("attachments")) {
            JSONArray attachments = refJson.getJSONArray("attachments");
            for (int i = 0; i < attachments.length(); i++) {
                String attachment = attachments.getString(i);
                document.add(new Field("attachments", attachment, Field.Store.YES, Field.Index.UN_TOKENIZED));
            }
        }

        if (refJson.has("paths")) {
            JSONArray paths = refJson.getJSONArray("paths");
            for (int i = 0; i < paths.length(); i++) {
                String path = batchItem.getRootUri() + paths.getString(i);
                document.add(new Field("paths", path, Field.Store.YES, Field.Index.UN_TOKENIZED));
                addPath(path, batchItem, null);
            }
        }

        synchronized (this) {
            indexTemplate.addDocument(document);
        }
    }

    public void deleteIndex(BatchItem batchItem) throws Exception {
        log.fine("deleteIndex(" + batchItem.getBatchUri() + ")");

        synchronized (this) {
            try {
                indexTemplate.deleteDocuments(new Term("batch", batchItem.getBatchUri()));
            } catch (Exception e) {
                log.warning("deleteIndex(" + batchItem.getBatchUri() + "): deleting existing index:" + e);
            }
        }
    }

    /*
     * Private Methods
     */

    private void addPath(String path, BatchItem batchItem, String uri) {
        if (!isEmpty(uri)) {
            Document document = new Document();
            document.add(new Field("uri", uri, Field.Store.YES, Field.Index.UN_TOKENIZED));
            document.add(new Field("paths", path, Field.Store.YES, Field.Index.UN_TOKENIZED));
            document.add(new Field("batch", batchItem.getBatchUri(), Field.Store.YES, Field.Index.UN_TOKENIZED));
            document.add(new Field("name", substringAfterLast(uri, "/"), Field.Store.YES, Field.Index.UN_TOKENIZED));
            synchronized (this) {
                indexTemplate.addDocument(document);
            }
        }

        if (contains(path, "/")) {
            String parentPath = substringBeforeLast(path, "/");
            if (!isEmpty(parentPath)) {
                addPath(parentPath, batchItem, path);
            }
        }
    }
}
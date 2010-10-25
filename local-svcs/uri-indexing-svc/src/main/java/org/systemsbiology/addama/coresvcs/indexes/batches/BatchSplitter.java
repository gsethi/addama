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

import org.json.JSONObject;

import java.util.List;

/**
 * @author hrovira
 */
public class BatchSplitter implements Runnable {
    private final BatchCallback batchCallback;
    private final List<BatchItem> batchItems;
    private final List<JSONObject> refJsons;

    public BatchSplitter(BatchCallback callback, List<BatchItem> items, List<JSONObject> list) {
        this.batchCallback = callback;
        this.batchItems = items;
        this.refJsons = list;
    }

    /*
     * Runnable
     */

    public void run() {
        for (BatchItem batchItem : batchItems) {
            try {
                batchItem.deleting();
                batchCallback.deleteIndex(batchItem);

                batchItem.indexing();
                for (JSONObject refJson : refJsons) {
                    batchCallback.createIndex(batchItem, refJson);
                    batchItem.incrementIndexCount();
                }

                batchItem.complete();
            } catch (Exception e) {
                batchItem.fail(e);
            }
        }
    }
}

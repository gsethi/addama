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
package org.systemsbiology.addama.indexes.batches;

import java.util.Calendar;

/**
 * @author hrovira
 */
public class BatchItem {
    private final String batchUri;
    private final String rootUri;
    private final long totalCount;
    private String status;
    private String message;
    private boolean finished;
    private int indexCount;

    public BatchItem(String batchUri, String rootUri, long totalIndexes) {
        this.batchUri = batchUri;
        this.rootUri = rootUri;
        this.totalCount = totalIndexes;
        this.status = "Pending";
        this.message = "Started at " + Calendar.getInstance().getTime();
    }

    public String getBatchUri() {
        return batchUri;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getRootUri() {
        return this.rootUri;
    }

    /*
     * Public Methods
     */

    public void deleting() {
        this.status = "Deleting";
        this.message = "Started at " + Calendar.getInstance().getTime();
    }

    public void indexing() {
        this.status = "Indexing";
        this.message = "Started at " + Calendar.getInstance().getTime();
    }

    public void complete() {
        this.status = "Completed";
        this.message = "Completed at " + Calendar.getInstance().getTime();
        this.finished = true;
    }

    public void fail(Exception e) {
        this.status = "Failed";
        this.message = e.getMessage();
        this.finished = true;
    }

    public void incrementIndexCount() {
        this.indexCount++;
    }
}

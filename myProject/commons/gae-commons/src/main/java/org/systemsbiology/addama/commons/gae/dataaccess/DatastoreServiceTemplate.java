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
package org.systemsbiology.addama.commons.gae.dataaccess;

import com.google.appengine.api.datastore.*;

/**
 * @author hrovira
 */
public class DatastoreServiceTemplate {
    private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    public void inTransaction(TransactionCallback callback) {
        Transaction x = datastoreService.beginTransaction();
        try {
            callback.execute(datastoreService, x);
        } finally {
            x.commit();
        }
    }

    public Entity getEntityByKey(Key k) throws EntityNotFoundException {
        return datastoreService.get(k);
    }

    public PreparedQuery prepare(Query query) {
        return datastoreService.prepare(query);
    }

    public DatastoreService getDatastoreService() {
        return datastoreService;
    }

}
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
package org.systemsbiology.addama.appengine.servlet;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.users.UserService;
import org.systemsbiology.addama.appengine.datastore.DeleteEntityTransactionCallback;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static com.google.appengine.api.users.UserServiceFactory.getUserService;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.systemsbiology.addama.appengine.datastore.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class DropEntityServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(DropEntityServlet.class.getName());

    private final UserService userService = getUserService();
    private final DatastoreService datastore = getDatastoreService();
    private final Queue queue = getDefaultQueue();

    private final String HEADER_NAME = "x-addama-dropentity-header";
    private final UUID ACCESS_KEY = randomUUID();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String requestUri = request.getRequestURI();
        log.info("doPost(" + requestUri + ")");

        String entityName = substringAfterLast(requestUri, "/entities/");

        PreparedQuery pq = datastore.prepare(new Query(entityName));
        int count = pq.countEntities();
        if (count == 0) {
            log.info("should have completed");
            return;
        }

        log.info("delete:" + count);
        if (count > 100) {
            count = 100;
        }

        Key[] keys = new Key[count];
        Iterator<Entity> itr = pq.asIterator();
        for (int i = 0; i < keys.length; i++) {
            if (itr.hasNext()) {
                Entity e = itr.next();
                keys[i] = e.getKey();
            }
        }

        for (Key k : keys) {
            if (k != null) {
                inTransaction(datastore, new DeleteEntityTransactionCallback(k));
            }
        }

        log.info("finished: " + count);

        log.info("dispatching task: " + requestUri);
        queue.add(withUrl(requestUri).header(HEADER_NAME, ACCESS_KEY.toString()));
    }

    /*
     * Private Methods
     */

    private boolean isValid(HttpServletRequest request) {
        if (userService.isUserLoggedIn() && userService.isUserAdmin()) {
            return true;
        }

        String actualValue = request.getHeader(HEADER_NAME);
        if (isEmpty(actualValue)) {
            return false;
        }

        return ACCESS_KEY.equals(fromString(actualValue));
    }
}

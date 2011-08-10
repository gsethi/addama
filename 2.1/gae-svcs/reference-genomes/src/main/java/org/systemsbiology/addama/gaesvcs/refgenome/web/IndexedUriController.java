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
package org.systemsbiology.addama.gaesvcs.refgenome.web;

import com.google.appengine.api.datastore.*;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.commons.web.views.JsonItemsView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class IndexedUriController extends AbstractController {
    private static final Logger log = Logger.getLogger(IndexedUriController.class.getName());

    private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        Query q = new Query("indexed-uris").addFilter("uri", Query.FilterOperator.EQUAL, requestUri);
        Iterator<Entity> itr = datastoreService.prepare(q).asIterator();
        if (!itr.hasNext()) {
            throw new ResourceNotFoundException(requestUri);
        }

        JSONObject json = new JSONObject();
        while (itr.hasNext()) {
            Entity e = itr.next();
            Text t = (Text) e.getProperty("json");
            json.append("items", new JSONObject(t.getValue()));
        }

        return new ModelAndView(new JsonItemsView()).addObject("json", json);
    }

}
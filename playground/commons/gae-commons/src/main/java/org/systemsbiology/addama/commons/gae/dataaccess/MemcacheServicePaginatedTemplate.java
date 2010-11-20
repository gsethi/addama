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

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.util.logging.Logger;

/**
 * Allow for memcache to hold a paginated view of a particular resource.  This is accomplished by 
 * placing all pages of a particular resource in the same memcache namespace with each page of 
 * the resource stored as a separate entry in memcache.  Allow for clearing of a particular 
 * page if the caller knows for example that one item on that page changed.  Also allow for clearing 
 * of all pages if an item was added or removed from the paged resource causing all the pages to reflow.
 * 
 * Implementation Detail: The resource key is used as a memcache namespace, therefore it is limited to 
 * 100 characters.  We could potentially pass in strategies for converting the resource key to an 
 * namespace if we find this too limiting. 
 * 
 * @author hrovira
 */
public class MemcacheServicePaginatedTemplate {
    private static final Logger log = Logger.getLogger(MemcacheServicePaginatedTemplate.class.getName());

    public Object loadIfNotExisting(String key, int limit, int offset, MemcachePaginatedLoaderCallback callback) throws Exception {
        return loadIfNotExisting(key, limit, offset, callback, Expiration.byDeltaSeconds(3600), SetPolicy.SET_ALWAYS);
    }

    public Object loadIfNotExisting(String key, int limit, int offset, MemcachePaginatedLoaderCallback callback, Expiration expiration, SetPolicy setPolicy) throws Exception {
    	String memcacheKey = makeMemcacheKey(key, limit, offset);
    	
    	log.fine("loadIfNotExisting(" + memcacheKey + "," + callback + "," + expiration + "," + setPolicy + ")");

    	// Use the key for the memcache namespace and the page-relevant data for the item key
        MemcacheService memcache = MemcacheServiceFactory.getMemcacheService(makeNamespaceKey(key));

        Object existing = memcache.get(memcacheKey);
        if (existing != null) {
            log.info("loadIfNotExisting(" + memcacheKey + "):existing");
            return existing;
        }

        Object tocache = callback.getCacheableObject(key, limit, offset);
        if (tocache != null) {
            log.fine("loadIfNotExisting(" + memcacheKey + "):storing");
            memcache.put(memcacheKey, tocache, expiration, setPolicy);
        } else {
            log.fine("loadIfNotExisting(" + memcacheKey + "):no cached object");
        }
        return tocache;
    }

    public void clearMemcachePage(String key, int limit, int offset) {
        MemcacheService memcache = MemcacheServiceFactory.getMemcacheService(makeNamespaceKey(key));
        String memcacheKey = makeMemcacheKey(key, limit, offset);
        if (memcache.contains(memcacheKey)) {
            memcache.delete(memcacheKey);
        }
    }

    public void clearMemcacheAllPages(String key) {
        MemcacheService memcache = MemcacheServiceFactory.getMemcacheService(makeNamespaceKey(key));
        memcache.clearAll();
    }
    
    private String makeNamespaceKey(String key) {
    	// Namespaces must match the pattern '[0-9A-Za-z._-]{0,100}'
    	return key.replace('/', '_');
    }
    
    private String makeMemcacheKey(String key, int limit, int offset) {
    	return key + ":" + limit + ":" + offset;
    }
}

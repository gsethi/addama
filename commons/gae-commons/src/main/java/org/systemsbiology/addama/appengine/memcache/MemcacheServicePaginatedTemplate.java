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
package org.systemsbiology.addama.appengine.memcache;

import com.google.appengine.api.memcache.MemcacheService;

import java.util.logging.Logger;

import static com.google.appengine.api.memcache.Expiration.byDeltaSeconds;
import static com.google.appengine.api.memcache.MemcacheService.SetPolicy.SET_ALWAYS;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;

/**
 * Allow for memcache to hold a paginated view of a particular resource.  This is accomplished by
 * placing all pages of a particular resource in the same memcache namespace with each page of
 * the resource stored as a separate entry in memcache.  Allow for clearing of a particular
 * page if the caller knows for example that one item on that page changed.  Also allow for clearing
 * of all pages if an item was added or removed from the paged resource causing all the pages to reflow.
 * <p/>
 * Implementation Detail: The resource key is used as a memcache namespace, therefore it is limited to
 * 100 characters.  We could potentially pass in strategies for converting the resource key to an
 * namespace if we find this too limiting.
 *
 * @author hrovira
 */
public class MemcacheServicePaginatedTemplate {
    private static final Logger log = Logger.getLogger(MemcacheServicePaginatedTemplate.class.getName());

    public static Object loadIfNotExist(String key, int limit, int offset, MemcachePaginatedLoaderCallback callback) throws Exception {
        // Use the key for the memcache namespace and the page-relevant data for the item key
        MemcacheService memcache = namespacedCache(key);
        String paginatedKey = paginatedKey(key, limit, offset);

        Object existing = memcache.get(paginatedKey);
        if (existing != null) {
            log.fine(paginatedKey + ":existing");
            return existing;
        }

        Object tocache = callback.getCacheableObject(key, limit, offset);
        if (tocache != null) {
            log.fine(paginatedKey + ":storing");
            memcache.put(paginatedKey, tocache, byDeltaSeconds(3600), SET_ALWAYS);
        }
        return tocache;
    }

    public static MemcacheService namespacedCache(String key) {
        // Namespaces must match the pattern '[0-9A-Za-z._-]{0,100}'
        return getMemcacheService(key.replace('/', '_'));
    }

    public static void clearPage(String key, int limit, int offset) {
        MemcacheService memcache = namespacedCache(key);
        String memcacheKey = paginatedKey(key, limit, offset);
        if (memcache.contains(memcacheKey)) {
            memcache.delete(memcacheKey);
        }
    }

    /*
     * Private Methods
     */

    private static String paginatedKey(String key, int limit, int offset) {
        return key + ":" + limit + ":" + offset;
    }
}

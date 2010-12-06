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
 * @author hrovira
 */
public class MemcacheServiceTemplate {
    private static final Logger log = Logger.getLogger(MemcacheServiceTemplate.class.getName());

    public Object loadIfNotExisting(String key, MemcacheLoaderCallback callback) throws Exception {
        return loadIfNotExisting(key, callback, Expiration.byDeltaSeconds(3600), SetPolicy.SET_ALWAYS);
    }

    public Object loadIfNotExisting(String key, MemcacheLoaderCallback callback, Expiration expiration, SetPolicy setPolicy) throws Exception {
        log.fine("loadIfNotExisting(" + key + "," + callback + "," + expiration + "," + setPolicy + ")");

        MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

        Object existing = memcache.get(key);
        if (existing != null) {
            log.info("loadIfNotExisting(" + key + "):existing");
            return existing;
        }

        Object tocache = callback.getCacheableObject(key);
        if (tocache != null) {
            log.fine("loadIfNotExisting(" + key + "):storing");
            memcache.put(key, tocache, expiration, setPolicy);
        } else {
            log.fine("loadIfNotExisting(" + key + "):no cached object");
        }
        return tocache;
    }

    public void clearMemcache(String key) {
        MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
        if (memcache.contains(key)) {
            memcache.delete(key);
        }
    }
}

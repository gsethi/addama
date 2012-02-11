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

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class MemcacheServiceTemplate {
    private static final Logger log = Logger.getLogger(MemcacheServiceTemplate.class.getName());

    public static Object loadIfNotExisting(MemcacheService memcache, String key,
                                           MemcacheLoaderCallback callback) throws Exception {
        return loadIfNotExisting(memcache, key, callback, null, null);
    }

    public static Object loadIfNotExisting(MemcacheService memcache, String key,
                                           MemcacheLoaderCallback callback,
                                           Expiration expiration) throws Exception {
        return loadIfNotExisting(memcache, key, callback, expiration, null);
    }

    public static Object loadIfNotExisting(MemcacheService memcache, String key,
                                           MemcacheLoaderCallback callback,
                                           Expiration expiration, SetPolicy setPolicy) throws Exception {
        log.fine(key + "," + expiration + "," + setPolicy);

        Object existing = memcache.get(key);
        if (existing != null) {
            log.fine(key + "," + expiration + "," + setPolicy + ": exists");
            return existing;
        }

        Object tocache = callback.getCacheableObject(key);
        if (tocache != null) {
            log.fine(key + "," + expiration + "," + setPolicy + ": storing");
            if (expiration != null) {
                if (setPolicy != null) {
                    memcache.put(key, tocache, expiration, setPolicy);
                } else {
                    memcache.put(key, tocache, expiration);
                }
            } else {
                memcache.put(key, tocache);
            }
        } else {
            log.fine(key + "," + expiration + "," + setPolicy + ": no cached object");
        }
        return tocache;
    }

    public static void clearMemcache(MemcacheService memcache, String key) {
        if (memcache.contains(key)) {
            memcache.delete(key);
        }
    }

}

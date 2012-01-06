package org.systemsbiology.addama.appengine.memcache;

import java.io.Serializable;

/**
 * @author hrovira
 */
public interface MemcacheLoaderCallback {
    public Serializable getCacheableObject(String key) throws Exception;
}
